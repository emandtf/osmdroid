/**
 * The MIT License
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * <p>
 * This code was sourced from the National Geospatial Intelligency Agency and was
 * originally licensed under the MIT license. It has been modified to support
 * osmdroid's APIs.
 * <p>
 * You can find the original code base here:
 * https://github.com/ngageoint/geopackage-android-map
 * https://github.com/ngageoint/geopackage-android
 */

package org.osmdroid.gpkg.overlay;


import android.util.Log;

import org.osmdroid.api.IMapView;
import org.osmdroid.gpkg.R;
import org.osmdroid.gpkg.overlay.features.MarkerOptions;
import org.osmdroid.gpkg.overlay.features.MultiLatLng;
import org.osmdroid.gpkg.overlay.features.MultiMarker;
import org.osmdroid.gpkg.overlay.features.MultiPolyline;
import org.osmdroid.gpkg.overlay.features.MultiPolylineOptions;
import org.osmdroid.gpkg.overlay.features.OsmDroidMapShape;
import org.osmdroid.gpkg.overlay.features.OsmMapShapeType;
import org.osmdroid.gpkg.overlay.features.PolygonOptions;
import org.osmdroid.gpkg.overlay.features.PolylineOptions;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import mil.nga.geopackage.GeoPackageException;

import mil.nga.proj.Projection;
import mil.nga.proj.ProjectionConstants;
import mil.nga.proj.ProjectionTransform;
import mil.nga.sf.CircularString;
import mil.nga.sf.CompoundCurve;
import mil.nga.sf.Curve;
import mil.nga.sf.CurvePolygon;
import mil.nga.sf.Geometry;
import mil.nga.sf.GeometryCollection;
import mil.nga.sf.GeometryType;
import mil.nga.sf.LineString;
import mil.nga.sf.MultiLineString;
import mil.nga.sf.MultiPoint;
import mil.nga.sf.MultiPolygon;
import mil.nga.sf.Point;
import mil.nga.sf.Polygon;
import mil.nga.sf.PolyhedralSurface;
import mil.nga.sf.TIN;
import mil.nga.sf.Triangle;

/*
 * created on 8/19/2017.
 *
 * @author Alex O'Ree
 */

/**
 * Provides conversions methods between geometry object and Google Maps Android
 * API v2 Shapes
 *
 * @author osbornb
 */
public class OsmMapShapeConverter {

    /**
     * Projection
     */
    private final Projection projection;

    /**
     * Transformation to WGS 84
     */
    private final ProjectionTransform toWgs84;

    /**
     * Transformation from WGS 84
     */
    private final ProjectionTransform fromWgs84;

    /**
     * Constructor
     *
     * @since 1.3.2
     */
    public OsmMapShapeConverter() {
        this(null, null, null, null);
    }

    private final MarkerOptions makerOptions;
    private final PolylineOptions polylineOptions;
    private final PolygonOptions polygonOptions;

    /** Constructor with specified projection */
    public OsmMapShapeConverter(Projection projection, MarkerOptions options, PolylineOptions polylineOptions, PolygonOptions polygonOptions) {
        Log.i(IMapView.LOGTAG, "Geopackage support is BETA. Please report any issues");
        this.projection = projection;
        this.polylineOptions = polylineOptions;
        this.polygonOptions = polygonOptions;
        this.makerOptions = options;
        if (projection != null) {
            toWgs84 = projection.getTransformation(ProjectionConstants.EPSG_WORLD_GEODETIC_SYSTEM);
            Projection wgs84 = toWgs84.getToProjection();
            fromWgs84 = wgs84.getTransformation(projection);
        } else {
            toWgs84 = null;
            fromWgs84 = null;
        }
    }

    /** Get the projection */
    public Projection getProjection() {
        return projection;
    }

    /** Transform a projection point to WGS84 */
    public Point toWgs84(@NonNull final Point point) {
        return toWgs84(point, null);
    }
    /** Transform a projection point to WGS84 */
    public Point toWgs84(@NonNull final Point point, @Nullable Point reuse) {
        if (projection != null) {
            final double[] transformedPoint = toWgs84.transform(point.getX(), point.getY());
            if (reuse == null) reuse = new Point(transformedPoint[0], transformedPoint[1]);
            else {
                reuse.setX(transformedPoint[0]);
                reuse.setY(transformedPoint[1]);
            }
        }
        return reuse;
    }

    /** Transform a WGS84 point to the projection */
    public Point toProjection(@NonNull Point point) {
        return toProjection(point, null);
    }
    /** Transform a WGS84 point to the projection */
    public Point toProjection(@NonNull final Point point, @Nullable Point reuse) {
        if (projection != null) {
            double[] transformedPoint = toWgs84.transform(point.getX(), point.getY());
            if (reuse == null) reuse = new Point(transformedPoint[0], transformedPoint[1]);
            else {
                reuse.setX(transformedPoint[0]);
                reuse.setY(transformedPoint[1]);
            }
        }
        return reuse;
    }

    /** Convert a {@link Point} to a {@link GeoPoint} */
    public GeoPoint toLatLng(@NonNull Point point) {
        return toLatLng(point, null, null);
    }
    /** Convert a {@link Point} to a {@link GeoPoint} */
    public GeoPoint toLatLng(@NonNull Point point, @Nullable final Point reusePoint, @Nullable GeoPoint reuseGeoPoint) {
        point = toWgs84(point, reusePoint);
        if (reuseGeoPoint == null) reuseGeoPoint = new GeoPoint(point.getY(), point.getX());
        else reuseGeoPoint.setCoords(point.getY(), point.getX());
        return reuseGeoPoint;
    }

    /** Convert a {@link LineString} to a {@link PolylineOptions} */
    public Polyline toPolyline(@NonNull final LineString lineString) {
        final Polyline line = new Polyline();
        if (polylineOptions != null) {
            line.setTitle(polylineOptions.getTitle());
            line.getOutlinePaint().setColor(polylineOptions.getColor());
            line.setGeodesic(polylineOptions.isGeodesic());
            line.getOutlinePaint().setStrokeWidth(polylineOptions.getWidth());
            line.setSubDescription(polylineOptions.getSubtitle());
        }
        final List<Point> cPoints = lineString.getPoints();
        final List<GeoPoint> pts = new ArrayList<>(cPoints.size());
        for (Point point : cPoints) pts.add(toLatLng(point));
        line.setPoints(pts);
        return line;
    }

    /** Convert a {@link Polygon} to a {@link PolygonOptions} */
    public org.osmdroid.views.overlay.Polygon toPolygon(Polygon polygon) {
        final org.osmdroid.views.overlay.Polygon newPoloygon = new org.osmdroid.views.overlay.Polygon();
        final List<GeoPoint> pts = new ArrayList<>();
        final List<List<GeoPoint>> holes = new ArrayList<>();
        final List<LineString> rings = polygon.getRings();
        if (!rings.isEmpty()) {
            Double z = null;
            // Add the polygon points
            LineString polygonLineString = rings.get(0);
            for (Point point : polygonLineString.getPoints()) {
                GeoPoint latLng = toLatLng(point);
                pts.add(latLng);
            }
            // Add the holes
            for (int i = 1; i < rings.size(); i++) {
                LineString hole = rings.get(i);
                List<GeoPoint> holeLatLngs = new ArrayList<>();
                for (Point point : hole.getPoints()) {
                    GeoPoint latLng = toLatLng(point);
                    holeLatLngs.add(latLng);
                    if (point.hasZ()) {
                        z = (z == null) ? point.getZ() : Math.max(z, point.getZ());
                    }
                }
                holes.add(holeLatLngs);
            }
        }
        newPoloygon.setPoints(pts);
        newPoloygon.setHoles(holes);
        if (polygonOptions != null) {
            newPoloygon.getFillPaint().setColor(polygonOptions.getFillColor());
            newPoloygon.getOutlinePaint().setColor(polygonOptions.getStrokeColor());
            newPoloygon.getOutlinePaint().setStrokeWidth(polygonOptions.getStrokeWidth());
            newPoloygon.setTitle(polygonOptions.getTitle());
        }
        return newPoloygon;
    }

    /**
     * Convert a {@link CurvePolygon} to a {@link PolygonOptions}
     *
     * @param curvePolygon curve polygon
     * @return polygon options
     * @since 1.4.1
     * @noinspection rawtypes
     */
    public org.osmdroid.views.overlay.Polygon toCurvePolygon(CurvePolygon curvePolygon) {
        final org.osmdroid.views.overlay.Polygon polygonOptions = new org.osmdroid.views.overlay.Polygon();
        final List<GeoPoint> pts = new ArrayList<>();
        final List<Curve> rings = curvePolygon.getRings();
        final List<List<GeoPoint>> holes = new ArrayList<>();
        if (!rings.isEmpty()) {
            Double z = null;

            // Add the polygon points
            Curve curve = rings.get(0);
            if (curve instanceof CompoundCurve) {
                final CompoundCurve compoundCurve = (CompoundCurve) curve;
                for (LineString lineString : compoundCurve.getLineStrings()) {
                    for (Point point : lineString.getPoints()) {
                        GeoPoint latLng = toLatLng(point);
                        pts.add(latLng);
                    }
                }
            } else if (curve instanceof LineString) {
                final LineString lineString = (LineString) curve;
                for (Point point : lineString.getPoints()) {
                    GeoPoint latLng = toLatLng(point);
                    pts.add(latLng);
                }
            } else {
                throw new GeoPackageException("Unsupported Curve Type: " + curve.getClass().getSimpleName());
            }

            // Add the holes
            for (int i = 1; i < rings.size(); i++) {
                Curve hole = rings.get(i);
                List<GeoPoint> holeLatLngs = new ArrayList<>();
                if (hole instanceof CompoundCurve) {
                    CompoundCurve holeCompoundCurve = (CompoundCurve) hole;
                    for (LineString holeLineString : holeCompoundCurve.getLineStrings()) {
                        for (Point point : holeLineString.getPoints()) {
                            GeoPoint latLng = toLatLng(point);
                            holeLatLngs.add(latLng);
                        }
                    }
                } else if (hole instanceof LineString) {
                    LineString holeLineString = (LineString) hole;
                    for (Point point : holeLineString.getPoints()) {
                        GeoPoint latLng = toLatLng(point);
                        holeLatLngs.add(latLng);
                        if (point.hasZ()) {
                            z = (z == null) ? point.getZ() : Math.max(z, point.getZ());
                        }
                    }
                } else {
                    throw new GeoPackageException("Unsupported Curve Hole Type: " + hole.getClass().getSimpleName());
                }
                holes.add(holeLatLngs);
            }
        }
        polygonOptions.setHoles(holes);
        polygonOptions.setPoints(pts);
        return polygonOptions;
    }


    /** Convert a {@link MultiPoint} to a {@link MultiLatLng} */
    public MultiLatLng toLatLngs(MultiPoint multiPoint) {
        final MultiLatLng multiLatLng = new MultiLatLng();
        for (Point point : multiPoint.getPoints()) {
            GeoPoint latLng = toLatLng(point);
            multiLatLng.add(latLng);
        }
        return multiLatLng;
    }


    /** Convert a {@link MultiLineString} to a {@link MultiPolylineOptions} */
    public List<Polyline> toPolylines(MultiLineString multiLineString) {
        final List<Polyline> lines = new ArrayList<>();
        for (LineString lineString : multiLineString.getLineStrings()) {
            Polyline polyline = toPolyline(lineString);
            lines.add(polyline);
        }
        return lines;
    }


    /** Convert a {@link MultiPolygon} to a {@link Polygon} */
    public List<org.osmdroid.views.overlay.Polygon> toPolygons(MultiPolygon multiPolygon) {
        final List<org.osmdroid.views.overlay.Polygon> polygons = new ArrayList<>();
        for (Polygon polygon : multiPolygon.getPolygons()) {
            org.osmdroid.views.overlay.Polygon polygonOptions = toPolygon(polygon);
            polygons.add(polygonOptions);
        }
        return polygons;
    }


    /** Convert a {@link CompoundCurve} to a {@link MultiPolylineOptions} */
    public List<Polyline> toPolylines(CompoundCurve compoundCurve) {
        final List<Polyline> lines = new ArrayList<>();
        for (LineString lineString : compoundCurve.getLineStrings()) {
            Polyline polyline = toPolyline(lineString);
            lines.add(polyline);
        }
        return lines;
    }


    /** Convert a {@link PolyhedralSurface} to a {@link Polygon} */
    public List<org.osmdroid.views.overlay.Polygon> toPolygons(PolyhedralSurface polyhedralSurface) {
        final List<org.osmdroid.views.overlay.Polygon> polygons = new ArrayList<>();
        for (Polygon polygon : polyhedralSurface.getPolygons()) {
            org.osmdroid.views.overlay.Polygon polygon1 = toPolygon(polygon);
            polygons.add(polygon1);
        }
        return polygons;
    }

    /** Convert a {@link Geometry} to a Map shape */
    public OsmDroidMapShape toShape(Geometry geometry) {
        OsmDroidMapShape shape = null;
        GeometryType geometryType = geometry.getGeometryType();
        switch (geometryType) {
            case POINT: {
                shape = new OsmDroidMapShape(geometryType, OsmMapShapeType.LAT_LNG, toLatLng((Point) geometry));
                break;
            }
            case LINESTRING: {
                shape = new OsmDroidMapShape(geometryType, OsmMapShapeType.POLYLINE_OPTIONS, toPolyline((LineString) geometry));
                break;
            }
            case POLYGON: {
                shape = new OsmDroidMapShape(geometryType, OsmMapShapeType.POLYGON_OPTIONS, toPolygon((Polygon) geometry));
                break;
            }
            case MULTIPOINT: {
                shape = new OsmDroidMapShape(geometryType, OsmMapShapeType.MULTI_LAT_LNG, toLatLngs((MultiPoint) geometry));
                break;
            }
            case MULTILINESTRING: {
                shape = new OsmDroidMapShape(geometryType, OsmMapShapeType.MULTI_POLYLINE_OPTIONS, toPolylines((MultiLineString) geometry));
                break;
            }
            case MULTIPOLYGON: {
                shape = new OsmDroidMapShape(geometryType, OsmMapShapeType.MULTI_POLYGON_OPTIONS, toPolygons((MultiPolygon) geometry));
                break;
            }
            case CIRCULARSTRING: {
                shape = new OsmDroidMapShape(geometryType, OsmMapShapeType.POLYLINE_OPTIONS, toPolyline((CircularString) geometry));
                break;
            }
            case COMPOUNDCURVE: {
                shape = new OsmDroidMapShape(geometryType, OsmMapShapeType.MULTI_POLYLINE_OPTIONS, toPolylines((CompoundCurve) geometry));
                break;
            }
            case CURVEPOLYGON: {
                shape = new OsmDroidMapShape(geometryType, OsmMapShapeType.POLYGON_OPTIONS, toCurvePolygon((CurvePolygon<?>) geometry));
                break;
            }
            case POLYHEDRALSURFACE: {
                shape = new OsmDroidMapShape(geometryType, OsmMapShapeType.MULTI_POLYGON_OPTIONS, toPolygons((PolyhedralSurface) geometry));
                break;
            }
            case TIN: {
                shape = new OsmDroidMapShape(geometryType, OsmMapShapeType.MULTI_POLYGON_OPTIONS, toPolygons((TIN) geometry));
                break;
            }
            case TRIANGLE: {
                shape = new OsmDroidMapShape(geometryType, OsmMapShapeType.POLYGON_OPTIONS, toPolygon((Triangle) geometry));
                break;
            }
            case GEOMETRYCOLLECTION: {
                //noinspection unchecked
                shape = new OsmDroidMapShape(geometryType, OsmMapShapeType.COLLECTION, toShapes((GeometryCollection<Geometry>) geometry));
                break;
            }
            default:
                throw new GeoPackageException("Unsupported Geometry Type: " + geometryType.getName());
        }

        return shape;
    }

    /** Convert a {@link GeometryCollection} to a list of Map shapes */
    public List<OsmDroidMapShape> toShapes(GeometryCollection<Geometry> geometryCollection) {
        List<OsmDroidMapShape> shapes = new ArrayList<>();
        for (Geometry geometry : geometryCollection.getGeometries()) {
            OsmDroidMapShape shape = toShape(geometry);
            shapes.add(shape);
        }
        return shapes;
    }

    /** Convert a {@link Geometry} to a Map shape and add it */
    @SuppressWarnings("unchecked")
    public OsmDroidMapShape addToMap(MapView map, Geometry geometry) {
        OsmDroidMapShape shape = null;
        GeometryType geometryType = geometry.getGeometryType();
        switch (geometryType) {
            case POINT: {
                shape = new OsmDroidMapShape(geometryType, OsmMapShapeType.MARKER, addLatLngToMap(map, toLatLng((Point) geometry)));
                break;
            }
            case LINESTRING: {
                shape = new OsmDroidMapShape(geometryType, OsmMapShapeType.POLYLINE, addPolylineToMap(map, toPolyline((LineString) geometry)));
                break;
            }
            case POLYGON: {
                shape = new OsmDroidMapShape(geometryType, OsmMapShapeType.POLYGON, addPolygonToMap(map, toPolygon((Polygon) geometry), polygonOptions));
                break;
            }
            case MULTIPOINT: {
                shape = new OsmDroidMapShape(geometryType, OsmMapShapeType.MULTI_MARKER, addLatLngsToMap(map, toLatLngs((MultiPoint) geometry)));
                break;
            }
            case MULTILINESTRING: {
                shape = new OsmDroidMapShape(geometryType, OsmMapShapeType.MULTI_POLYLINE, addPolylinesToMap(map, toPolylines((MultiLineString) geometry)));
                break;
            }
            case MULTIPOLYGON: {
                shape = new OsmDroidMapShape(geometryType, OsmMapShapeType.MULTI_POLYGON, addPolygonsToMap(map, toPolygons((MultiPolygon) geometry), polygonOptions));
                break;
            }
            case CIRCULARSTRING: {
                shape = new OsmDroidMapShape(geometryType, OsmMapShapeType.POLYLINE, addPolylineToMap(map, toPolyline((CircularString) geometry)));
                break;
            }
            case COMPOUNDCURVE: {
                shape = new OsmDroidMapShape(geometryType, OsmMapShapeType.MULTI_POLYLINE, addPolylinesToMap(map, toPolylines((CompoundCurve) geometry)));
                break;
            }
            case CURVEPOLYGON: {
                org.osmdroid.views.overlay.Polygon polygon = toCurvePolygon((CurvePolygon<?>) geometry);
                shape = new OsmDroidMapShape(geometryType, OsmMapShapeType.POLYGON, addPolygonToMap(map, polygon, polygonOptions));
                break;
            }
            case POLYHEDRALSURFACE: {
                shape = new OsmDroidMapShape(geometryType, OsmMapShapeType.MULTI_POLYGON, addPolygonsToMap(map, toPolygons((PolyhedralSurface) geometry), polygonOptions));
                break;
            }
            case TIN: {
                shape = new OsmDroidMapShape(geometryType, OsmMapShapeType.MULTI_POLYGON, addPolygonsToMap(map, toPolygons((TIN) geometry), polygonOptions));
                break;
            }
            case TRIANGLE: {
                shape = new OsmDroidMapShape(geometryType, OsmMapShapeType.POLYGON, addPolygonToMap(map, toPolygon((Triangle) geometry), polygonOptions));
                break;
            }
            case GEOMETRYCOLLECTION: {
                shape = new OsmDroidMapShape(geometryType, OsmMapShapeType.COLLECTION, addToMap(map, (GeometryCollection<Geometry>) geometry));
                break;
            }
            default:
                throw new GeoPackageException("Unsupported Geometry Type: " + geometryType.getName());
        }

        return shape;
    }

    /** Add a LatLng to the map */
    public static Marker addLatLngToMap(MapView map, GeoPoint latLng) {
        return addLatLngToMap(map, latLng, new MarkerOptions());
    }

    /** Add a LatLng to the map */
    public static Marker addLatLngToMap(MapView map, GeoPoint latLng, MarkerOptions options) {
        final Marker m = new Marker(map);
        m.setPosition(latLng);
        if (options != null) {
            if (options.getIcon() != null) {
                m.setIcon(options.getIcon());
            }
            m.setAlpha(options.getAlpha());
            m.setTitle(options.getTitle());
            m.setSubDescription(options.getSubdescription());
            m.setInfoWindow(new BasicInfoWindow(R.layout.bonuspack_bubble, map));
        }
        map.getOverlayManager().add(m);
        return m;
    }

    /** Add a Polyline to the map */
    public static Polyline addPolylineToMap(MapView map, Polyline polyline) {
        if (polyline.getInfoWindow() == null)
            polyline.setInfoWindow(new BasicInfoWindow(R.layout.bonuspack_bubble, map));
        map.getOverlayManager().add(polyline);
        return polyline;
    }

    /** Add a Polygon to the map */
    public static org.osmdroid.views.overlay.Polygon addPolygonToMap(MapView map,List<GeoPoint> pts, List<List<GeoPoint>> holes, PolygonOptions options) {
        final org.osmdroid.views.overlay.Polygon polygon1 = new org.osmdroid.views.overlay.Polygon(map);
        polygon1.setPoints(pts);
        polygon1.getHoles().addAll(holes);
        if (options != null) {
            polygon1.getFillPaint().setColor(options.getFillColor());
            polygon1.setTitle(options.getTitle());
            polygon1.getOutlinePaint().setColor(options.getStrokeColor());
            polygon1.getOutlinePaint().setStrokeWidth(options.getStrokeWidth());
            polygon1.setSubDescription(options.getSubtitle());
            polygon1.setInfoWindow(new BasicInfoWindow(R.layout.bonuspack_bubble, map));
        }
        map.getOverlayManager().add(polygon1);
        return polygon1;
    }


    /** Add a Polygon to the map */
    public static org.osmdroid.views.overlay.Polygon addPolygonToMap(MapView map, org.osmdroid.views.overlay.Polygon polygon, PolygonOptions options) {
        if (options != null) {
            polygon.getFillPaint().setColor(options.getFillColor());
            polygon.setTitle(options.getTitle());
            polygon.getOutlinePaint().setColor(options.getStrokeColor());
            polygon.getOutlinePaint().setStrokeWidth(options.getStrokeWidth());
            polygon.setSubDescription(options.getSubtitle());
            polygon.setInfoWindow(new BasicInfoWindow(R.layout.bonuspack_bubble, map));
        }
        map.getOverlayManager().add(polygon);
        return polygon;
    }

    /** Add a list of LatLngs to the map */
    public static MultiMarker addLatLngsToMap(MapView map, MultiLatLng latLngs) {
        final MultiMarker multiMarker = new MultiMarker();
        for (GeoPoint latLng : latLngs.getLatLngs()) {
            Marker marker = addLatLngToMap(map, latLng, latLngs.getMarkerOptions());
            multiMarker.add(marker);
        }
        return multiMarker;
    }

    /** Add a list of Polylines to the map */
    public static MultiPolyline addPolylinesToMap(MapView map, List<Polyline> polylines) {
        final MultiPolyline multiPolyline = new MultiPolyline();
        for (Polyline line : polylines) {
            if (line.getInfoWindow() == null)
                line.setInfoWindow(new BasicInfoWindow(R.layout.bonuspack_bubble, map));
            map.getOverlayManager().add(line);
            multiPolyline.add(line);
        }
        return multiPolyline;
    }

    public static org.osmdroid.gpkg.overlay.features.MultiPolygon addPolygonsToMap(MapView map, List<org.osmdroid.views.overlay.Polygon> polygons, PolygonOptions opts) {
        final org.osmdroid.gpkg.overlay.features.MultiPolygon multiPolygon = new org.osmdroid.gpkg.overlay.features.MultiPolygon();
        for (org.osmdroid.views.overlay.Polygon polygonOption : polygons) {
            org.osmdroid.views.overlay.Polygon polygon = addPolygonToMap(map, polygonOption.getActualPoints(), polygonOption.getHoles(), opts);
            if (polygon.getInfoWindow() == null)
                polygon.setInfoWindow(new BasicInfoWindow(R.layout.bonuspack_bubble, map));
            multiPolygon.add(polygon);
        }
        return multiPolygon;
    }

    /** Convert a {@link GeometryCollection} to a list of Map shapes and add to the map */
    public List<OsmDroidMapShape> addToMap(MapView map, GeometryCollection<Geometry> geometryCollection) {
        final List<OsmDroidMapShape> shapes = new ArrayList<>();
        for (Geometry geometry : geometryCollection.getGeometries()) {
            OsmDroidMapShape shape = addToMap(map, geometry);
            shapes.add(shape);
        }
        return shapes;
    }

}
