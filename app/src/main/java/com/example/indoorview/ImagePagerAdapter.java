package com.example.indoorview;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import java.util.List;

// Adapter para el ViewPager con soporte para zoom
public class ImagePagerAdapter extends androidx.viewpager.widget.PagerAdapter {
    private List<String> urls;
    private Context context;
    private LayoutInflater inflater;

    public ImagePagerAdapter(List<String> urls, Context context) {
        this.urls = urls;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return urls.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        View view = inflater.inflate(R.layout.item_visor_imagen, container, false);

        // Usar TouchImageView para soporte de zoom
        TouchImageView imageView = view.findViewById(R.id.iv_visor_imagen);

        // Cargar la imagen
        String url = urls.get(position);
        if (url != null && !url.isEmpty()) {
            try {
                Uri uri = Uri.parse(url);
                imageView.setImageURI(uri);
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            } catch (Exception e) {
                imageView.setImageResource(R.drawable.ic_placeholder);
                Log.e("VISOR", "Error cargando imagen: " + e.getMessage());
            }
        }

        // Doble tap para zoom (TouchImageView ya lo maneja)

        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }
}