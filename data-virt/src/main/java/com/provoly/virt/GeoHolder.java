package com.provoly.virt;

import java.util.Map;

import com.provoly.common.error.BusinessException;
import com.provoly.common.error.ErrorCode;
import com.provoly.common.item.GeoFormat;
import com.provoly.common.model.Type;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.*;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.postgresql.util.PGobject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import net.postgis.jdbc.jts.JtsGeometry;

@JsonSerialize(using = GeoHolderSerializer.class)
public class GeoHolder {

    public static final int PRECISION = 15;

    private static final ObjectMapper geoMapper = new ObjectMapper();
    private static final GeoJsonWriter geoJsonWriter = new GeoJsonWriter(PRECISION);
    private static final GeoJsonReader geoJsonReader = new GeoJsonReader();

    // We are using threadLocal as reader and writer are not thread safe and
    // allocate a readers and writers for every value in a response is a non-negligible memory consumption
    private final ThreadLocal<WKTReader> wktReader = ThreadLocal.withInitial(WKTReader::new);
    private final ThreadLocal<WKTWriter> wktWriter = ThreadLocal.withInitial(WKTWriter::new);
    private final ThreadLocal<WKBReader> wkbReader = ThreadLocal.withInitial(WKBReader::new);
    private final ThreadLocal<WKBWriter> wkbWriter = ThreadLocal.withInitial(WKBWriter::new);

    private Geometry jtsGeometry;
    private String geoJson;
    private Map<String, Object> geoMap;
    private byte[] wkb;
    private String wkt;
    private Integer srid;

    public GeoHolder(Geometry jtsGeometry) { // Use in shapefile walker, this import works
        this.jtsGeometry = jtsGeometry;
        this.srid = jtsGeometry.getSRID();
    }

    public GeoHolder(Object value, GeoFormat format, String crs) {
        switch (value) {
            case Map map -> {
                if (format != GeoFormat.GEO_JSON) {
                    throw new BusinessException(ErrorCode.TECHNICAL, "Bad map class " + value.getClass());
                }
                this.geoMap = map;
            }
            case String geoString -> {
                if (format == GeoFormat.GEO_JSON) {
                    throw new BusinessException(ErrorCode.TECHNICAL, "Bad string class " + value.getClass());
                }
                init(geoString, crs, format);
            }
            default -> throw new BusinessException(ErrorCode.TECHNICAL, "Unknown class " + value.getClass());
        }
    }

    public GeoHolder(Map<String, Object> geoMap) { // Use in elastic storage, override default storage will not work
        this.geoMap = geoMap;
    }

    public GeoHolder(String geoJson) {
        this(geoJson, GeoFormat.GEO_JSON);
    }

    public GeoHolder(String geoJson, String crs) {
        this(geoJson, crs, GeoFormat.GEO_JSON);
    }

    public GeoHolder(String geo, GeoFormat format) {
        this(geo, null, format);
    }

    public GeoHolder(String geo, String crs, GeoFormat format) {
        init(geo, crs, format);
    }

    private void init(String geo, String crs, GeoFormat format) {
        switch (format) {
            case GEO_JSON -> this.geoJson = geo;
            case WKT -> this.wkt = geo;
            default -> throw new BusinessException(ErrorCode.TECHNICAL, "Not supported geoFormat from String " + format);
        }
        if (crs != null) {
            this.srid = Integer.parseInt(crs.split(":")[1]);
        }
    }

    public Type getType() {
        ensureJtsGeometry();
        String geometryType = jtsGeometry.getGeometryType();
        return switch (geometryType) {
            case Geometry.TYPENAME_POINT -> Type.POINT;
            case Geometry.TYPENAME_MULTIPOINT -> Type.MULTIPOINT;
            case Geometry.TYPENAME_LINESTRING -> Type.LINESTRING;
            case Geometry.TYPENAME_MULTILINESTRING -> Type.MULTILINESTRING;
            case Geometry.TYPENAME_POLYGON -> Type.POLYGON;
            case Geometry.TYPENAME_MULTIPOLYGON -> Type.MULTIPOLYGON;
            default ->
                throw new BusinessException(ErrorCode.NOT_SUPPORTED, "GeometryCollection " + geometryType + " unsuported");
        };
    }

    public PGobject getAsPgObject(String crs) {
        this.srid = Integer.parseInt(crs.split(":")[1]);
        ensureJtsGeometry();
        return new JtsGeometry(jtsGeometry);
    }

    public void transformToMulti() {
        ensureJtsGeometry();

        jtsGeometry = switch (getType()) {
            case POINT -> new MultiPoint(new Point[] { (Point) jtsGeometry },
                    new GeometryFactory(jtsGeometry.getPrecisionModel(), jtsGeometry.getSRID()));
            case LINESTRING -> new MultiLineString(new LineString[] { (LineString) jtsGeometry },
                    new GeometryFactory(jtsGeometry.getPrecisionModel(), jtsGeometry.getSRID()));
            case POLYGON -> new MultiPolygon(new Polygon[] { (Polygon) jtsGeometry },
                    new GeometryFactory(jtsGeometry.getPrecisionModel(), jtsGeometry.getSRID()));
            case MULTIPOINT, MULTILINESTRING, MULTIPOLYGON -> jtsGeometry;
            default -> throw new BusinessException(ErrorCode.TECHNICAL, "Cannot transform %s to Multi".formatted(getType()));
        };
    }

    public String getStringAs(GeoFormat geoFormat) {
        ensureJtsGeometry();
        return switch (geoFormat) {
            case GEO_JSON -> geoJsonWriter.write(jtsGeometry);
            case WKB -> WKBWriter.toHex(wkbWriter.get().write(jtsGeometry));
            case WKT -> wktWriter.get().writeFormatted(jtsGeometry);
        };
    }

    public String toString() {
        return getStringAs(GeoFormat.GEO_JSON);
    }

    private void ensureJtsGeometry() {
        try {
            if (jtsGeometry != null)
                return;
            if (wkb != null) {
                jtsGeometry = wkbReader.get().read(wkb);
            } else if (wkt != null) {
                jtsGeometry = wktReader.get().read(wkt);
            } else if (geoJson != null) {
                jtsGeometry = geoJsonReader.read(geoJson);
            } else {
                throw new BusinessException(ErrorCode.TECHNICAL, "No convenient source format");
            }

            if (srid != null) {
                jtsGeometry.setSRID(srid);
            }

        } catch (ParseException e) {
            throw new BusinessException(ErrorCode.TECHNICAL, "Unable to convert to Jts", e);
        }
    }

    public Object getAsMap() {
        try {
            return geoMapper.readTree(toString());
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.TECHNICAL,
                    "Cannot create geometry from following structure : %s".formatted(toString()), e);
        }
    }
}
