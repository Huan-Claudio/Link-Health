package br.edu.pucgoias.app.ui.comum;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import java.io.*;

/** Reduz a imagem antes de alocar o bitmap e remove metadados pessoais ao recodificá-la. */
final class PhotoPreparation {
    private PhotoPreparation() { }
    static byte[] read(ContentResolver resolver, Uri uri, String mime) throws IOException {
        byte[] source;
        try (InputStream input = resolver.openInputStream(uri); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (input == null) throw new IOException("Imagem indisponível.");
            byte[] buffer = new byte[8192]; int n;
            while ((n = input.read(buffer)) != -1) {
                if (out.size() + n > 20 * 1024 * 1024) throw new IOException("Selecione uma imagem de até 20 MB.");
                out.write(buffer, 0, n);
            }
            source = out.toByteArray();
        }
        BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(source, 0, source.length, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0 || (long) bounds.outWidth * bounds.outHeight > 100_000_000)
            throw new IOException("A imagem não é válida ou suas dimensões são muito grandes.");
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / options.inSampleSize > 1600) options.inSampleSize *= 2;
        Bitmap image = BitmapFactory.decodeByteArray(source, 0, source.length, options);
        if (image == null) throw new IOException("Não foi possível ler a imagem.");
        try {
            int orientation = ExifInterface.ORIENTATION_NORMAL;
            try { orientation = new ExifInterface(new ByteArrayInputStream(source)).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL); }
            catch (IOException ignored) { }
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90: matrix.postRotate(90); break;
                case ExifInterface.ORIENTATION_ROTATE_180: matrix.postRotate(180); break;
                case ExifInterface.ORIENTATION_ROTATE_270: matrix.postRotate(270); break;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: matrix.setScale(-1, 1); break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL: matrix.setScale(1, -1); break;
                case ExifInterface.ORIENTATION_TRANSPOSE: matrix.setScale(-1, 1); matrix.postRotate(270); break;
                case ExifInterface.ORIENTATION_TRANSVERSE: matrix.setScale(-1, 1); matrix.postRotate(90); break;
            }
            if (!matrix.isIdentity()) {
                Bitmap oriented = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
                if (oriented != image) { image.recycle(); image = oriented; }
            }
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                Bitmap.CompressFormat format = "image/png".equals(mime) ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;
                if (!image.compress(format, 90, output)) throw new IOException("Falha ao preparar a imagem.");
                if (output.size() > 5 * 1024 * 1024) throw new IOException("A foto preparada ultrapassa 5 MB.");
                return output.toByteArray();
            }
        } finally { image.recycle(); }
    }
}
