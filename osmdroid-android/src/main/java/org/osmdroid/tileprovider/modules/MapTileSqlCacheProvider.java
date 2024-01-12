package org.osmdroid.tileprovider.modules;

import android.graphics.drawable.Drawable;
import android.util.Log;

import org.osmdroid.api.IMapView;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.IMapTileProviderCallback;
import org.osmdroid.tileprovider.IRegisterReceiver;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.osmdroid.tileprovider.tilesource.BitmapTileSourceBase;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.util.Counters;
import org.osmdroid.util.MapTileIndex;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Sqlite based tile cache mechansism
 *
 * @see SqlTileWriter
 * Created by alex on 1/16/16.
 * @since 5.1
 */
public class MapTileSqlCacheProvider extends MapTileFileStorageProviderBase {

    // ===========================================================
    // Constants
    // ===========================================================

    public static final String CONST_MAPTILEPROVIDER_SQLCACHE = "sqlcache";

    // ===========================================================
    // Fields
    // ===========================================================

    private final AtomicReference<ITileSource> mTileSource = new AtomicReference<>();
    private SqlTileWriter mWriter;
    private final TileLoader mTileLoader = new TileLoader();

    // ===========================================================
    // Constructors
    // ===========================================================

    /**
     * @deprecated Use instead: {@link #MapTileSqlCacheProvider(IRegisterReceiver, ITileSource)}
     */
    @Deprecated
    public MapTileSqlCacheProvider(final IRegisterReceiver pRegisterReceiver,
                                   final ITileSource pTileSource, final long pMaximumCachedFileAge) {
        this(pRegisterReceiver, pTileSource);
    }

    /**
     * The tiles may be found on several media. This one works with tiles stored on database.
     * It and its friends are typically created and controlled by {@link MapTileProviderBase}.
     */
    public MapTileSqlCacheProvider(final IRegisterReceiver pRegisterReceiver,
                                   final ITileSource pTileSource) {
        super(pRegisterReceiver,
                Configuration.getInstance().getTileFileSystemThreads(),
                Configuration.getInstance().getTileFileSystemMaxQueueSize());

        setTileSource(pTileSource);
        mWriter = new SqlTileWriter();
    }

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================

    @Override
    public boolean getUsesDataConnection() {
        return false;
    }

    @Override
    protected String getName() {
        return "SQL Cache Archive Provider";
    }

    @Override
    protected String getThreadGroupName() {
        return CONST_MAPTILEPROVIDER_SQLCACHE;
    }

    @Override
    public TileLoader getTileLoader() {
        return mTileLoader;
    }

    @Override
    public int getMinimumZoomLevel() {
        ITileSource tileSource = mTileSource.get();
        return tileSource != null ? tileSource.getMinimumZoomLevel() : OpenStreetMapTileProviderConstants.MINIMUM_ZOOMLEVEL;
    }

    @Override
    public int getMaximumZoomLevel() {
        ITileSource tileSource = mTileSource.get();
        return tileSource != null ? tileSource.getMaximumZoomLevel()
                : org.osmdroid.util.TileSystem.getMaximumZoomLevel();
    }

    @Override
    protected void onMediaMounted() { /*nothing*/ }

    @Override
    protected void onMediaUnmounted() {
        if (mWriter != null) mWriter.onDetach();
        mWriter = new SqlTileWriter();
    }

    @Override
    public void setTileSource(final ITileSource pTileSource) {
        mTileSource.set(pTileSource);
    }

    @Override
    public void detach() {
        if (mWriter != null) mWriter.onDetach();
        mWriter = null;
        super.detach();
    }

    // ===========================================================
    // Methods
    // ===========================================================

    /**
     * returns true if the given tile for the current map source exists in the cache db
     */
    public boolean hasTile(final long pMapTileIndex) {
        ITileSource tileSource = mTileSource.get();
        if (tileSource == null) return false;
        return mWriter.getExpirationTimestamp(tileSource, pMapTileIndex) != null;
    }


    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================


    protected class TileLoader extends MapTileModuleProviderBase.TileLoader {

        @Override
        public Drawable loadTile(final long pMapTileIndex) throws CantContinueException {

            ITileSource tileSource = mTileSource.get();
            if (tileSource == null) return null;

            if (mWriter != null) {
                try {
                    final Drawable result = mWriter.loadTile(tileSource, pMapTileIndex);
                    if (result == null) Counters.fileCacheMiss++;
                    else Counters.fileCacheHit++;
                    return result;
                } catch (final BitmapTileSourceBase.LowMemoryException e) {
                    // low memory so empty the queue
                    Log.w(IMapView.LOGTAG, "LowMemoryException downloading MapTile: " + MapTileIndex.toString(pMapTileIndex) + " : " + e);
                    Counters.fileCacheOOM++;
                    throw new CantContinueException(e);
                } catch (final Throwable e) {
                    Log.e(IMapView.LOGTAG, "Error loading tile", e);
                    return null;
                }
            } else {
                Log.d(IMapView.LOGTAG, "TileLoader failed to load tile due to mWriter being null (map shutdown?)");
            }
            return null;
        }

        @IMapTileProviderCallback.TILEPROVIDERTYPE
        @Override
        public final int getProviderType() { return IMapTileProviderCallback.TILEPROVIDERTYPE_SQL_CACHE; }
    }
}
