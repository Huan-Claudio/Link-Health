package br.edu.pucgoias.app.ui.nutricionista.edicao;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import br.edu.pucgoias.app.data.remote.RecommendedProductsApi;
import br.edu.pucgoias.app.model.Produto;

/** Mantém rascunhos, requisições e resultados mesmo quando a tela é recriada. */
public class ProdutosViewModel extends ViewModel {
    public static final class Estado {
        public final List<RascunhoProduto> produtos = new ArrayList<>();
        public boolean carregado;
        public boolean falhaInicial;
        public String mensagem;
    }

    private final Estado estado = new Estado();
    private final MutableLiveData<Estado> eventos = new MutableLiveData<>(estado);
    private final Handler main = new Handler(Looper.getMainLooper());
    private final RecommendedProductsApi api = new RecommendedProductsApi();
    private UUID patientId;
    private String token;
    private boolean iniciou;

    public LiveData<Estado> observar() { return eventos; }
    public Estado atual() { return estado; }

    public String consumirMensagem() {
        String mensagem = estado.mensagem;
        estado.mensagem = null;
        return mensagem;
    }

    public void carregar(UUID id, String accessToken) {
        if (iniciou) return;
        iniciou = true;
        patientId = id;
        token = accessToken;
        api.list(id, accessToken, new RecommendedProductsApi.ListCallback() {
            public void onSuccess(List<Produto> produtos) { main.post(() -> {
                for (Produto produto : produtos) estado.produtos.add(new RascunhoProduto(produto));
                estado.carregado = true;
                publicar(null);
            }); }
            public void onError(String message) { main.post(() -> {
                estado.falhaInicial = true;
                publicar(message);
            }); }
        });
    }

    public void adicionar() {
        if (!estado.carregado) return;
        estado.produtos.add(new RascunhoProduto(new Produto("", "", "")));
        publicar(null);
    }

    public void salvar(int posicao) {
        RascunhoProduto r = estado.produtos.get(posicao);
        if (r.pendente) return;
        if (r.nome.trim().isEmpty() || r.nome.trim().length() > 160) {
            publicar("Informe o nome do produto com até 160 caracteres."); return;
        }
        BigDecimal preco;
        try {
            preco = new BigDecimal(r.preco.trim().replace(',', '.'));
            if (preco.signum() < 0 || preco.scale() > 2
                    || preco.compareTo(new BigDecimal("9999999999.99")) > 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            publicar("Informe um preço válido, com até duas casas decimais."); return;
        }
        String link = r.link.trim();
        try {
            URI uri = URI.create(link);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getPort() > 65535 || uri.getPort() == 0 || link.length() > 2048) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            publicar("Informe um link HTTP ou HTTPS válido, sem usuário ou senha."); return;
        }
        r.pendente = true;
        publicar(null);
        api.save(patientId, r.original, r.nome.trim(), preco, link, token, new RecommendedProductsApi.ProductCallback() {
            public void onSuccess(Produto produto) { main.post(() -> {
                // O resultado pertence ao ViewModel, não à Activity antiga.
                r.original.setId(produto.getId());
                r.original.setNome(produto.getNome());
                r.original.setPreco(produto.getPreco());
                r.original.setLink(produto.getLink());
                r.original.setMoeda(produto.getMoeda());
                r.nome = produto.getNome();
                r.preco = produto.getPreco();
                r.link = produto.getLink();
                r.pendente = false;
                publicar("Produto salvo");
            }); }
            public void onError(String message) { main.post(() -> {
                r.pendente = false;
                publicar(message);
            }); }
        });
    }

    public void remover(int posicao) {
        RascunhoProduto r = estado.produtos.get(posicao);
        if (r.pendente) return;
        if (r.original.getId() == null) {
            estado.produtos.remove(r);
            publicar(null);
            return;
        }
        r.pendente = true;
        publicar(null);
        api.delete(patientId, r.original.getId(), token, new RecommendedProductsApi.DeleteCallback() {
            public void onSuccess() { main.post(() -> {
                estado.produtos.remove(r);
                publicar(null);
            }); }
            public void onError(String message) { main.post(() -> {
                r.pendente = false;
                publicar(message);
            }); }
        });
    }

    private void publicar(String mensagem) {
        if (mensagem != null) estado.mensagem = mensagem;
        eventos.setValue(estado);
    }
}
