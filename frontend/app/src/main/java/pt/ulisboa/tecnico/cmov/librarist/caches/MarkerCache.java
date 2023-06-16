package pt.ulisboa.tecnico.cmov.librarist.caches;


import android.util.LruCache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.maps.model.Marker;

public class MarkerCache {

    // Map of the libraries
    private final LruCache<Integer, Marker> markerCache;

    public MarkerCache(int cacheSize) {
        this.markerCache = new LruCache<Integer, Marker>(cacheSize) {
            @Override
            protected int sizeOf(Integer key, Marker marker) {
                return getObjectSize(marker);
            }
        };
    }

    private int getObjectSize(Object object) {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
            objectStream.writeObject(object);
            objectStream.flush();
            objectStream.close();
            return byteStream.size();
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public void addMarker(int libId, Marker marker){
        markerCache.put(libId, marker);
    }

    public void removeMarker(int libId){
        markerCache.remove(libId);
    }

    public Marker getMarker(int libId){
        return markerCache.get(libId);
    }

    public List<Marker> getMarkers(){
        return new ArrayList<>(this.markerCache.snapshot().values());
    }

    public void clearCache() {
        markerCache.evictAll();
    }
}

