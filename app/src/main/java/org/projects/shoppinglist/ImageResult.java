package org.projects.shoppinglist;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class ImageResult extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_result);

        /*byte[] bytes = getIntent().getByteArrayExtra("myimage");
        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        ImageView imageView = (ImageView)findViewById(R.id.imageView);
        imageView.setImageBitmap(bmp); */
    }
}
