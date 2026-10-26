package br.edu.pucgoias.app.ui.adapter;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import br.edu.pucgoias.app.R;

public final class ShoppingMessageAdapter extends RecyclerView.Adapter<ShoppingMessageAdapter.Holder> {
    private String message = "";
    private final Runnable retry;
    public ShoppingMessageAdapter(Runnable retry) { this.retry = retry; }
    public void update(String value) { message = value; notifyItemChanged(0); }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        TextView view = new TextView(parent.getContext());
        view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        view.setTextColor(parent.getContext().getColor(R.color.lh_text_secondary));
        view.setPadding(0, 16, 0, 24); view.setTextSize(13);
        view.setOnClickListener(v -> retry.run()); return new Holder(view);
    }
    @Override public void onBindViewHolder(@NonNull Holder h, int position) { ((TextView) h.itemView).setText(message); }
    @Override public int getItemCount() { return 1; }
    static final class Holder extends RecyclerView.ViewHolder { Holder(TextView view) { super(view); } }
}
