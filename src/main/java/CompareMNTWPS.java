package fr.indigeo.wps.clt;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.factory.StaticMethodsProcessFactory;
import org.geotools.text.Text;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

/**
 * @author Pierre Jego https://jdev.fr
 *
 */
@DescribeProcess(title = "Compare eleveation beetween two MNTS", description = "WPS tracking MNT differences")
public class CompareMNTWPS extends StaticMethodsProcessFactory<CompareMNTWPS> implements GeoServerProcess {

	private static final Logger LOGGER = Logger.getLogger(CompareMNTWPS.class);

	public CompareMNTWPS() {
		super(Text.text("WPS to compare MNT"), "mnt", CompareMNTWPS.class);
	}

	/**
	 * @param mnt1 reference 
	 * @param mnt2 to compare
	 * @return
	 */
	@DescribeProcess(title = "Compare MNT", description = "give Mnt poin")
	@DescribeResult(name = "resulFeatureCollection", description = "the result of drawing radials in reference Line")
	public static FeatureCollection<SimpleFeatureType, SimpleFeature> drawRadial(
			@DescribeParameter(name = "mnt1", description = "the input referenceLine") final FeatureCollection<SimpleFeatureType, SimpleFeature> mnt1,
			@DescribeParameter(name = "mnt2", description = "the input referenceLine") final FeatureCollection<SimpleFeatureType, SimpleFeature> mnt2 {
		
				DefaultFeatureCollection resultFeatureCollection = null;

		LOGGER.info("Compare mnt");


			// Init simple feature to add in collection
			SimpleFeatureTypeBuilder simpleFeatureTypeBuilder = new SimpleFeatureTypeBuilder();
			simpleFeatureTypeBuilder.setName("featureType");
			simpleFeatureTypeBuilder.add("geometry", Point.class);
			simpleFeatureTypeBuilder.add("elevationDiff", Double.class);

			// init DefaultFeatureCollection
			SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(simpleFeatureTypeBuilder.buildFeatureType());
			resultFeatureCollection = new DefaultFeatureCollection(null, simpleFeatureBuilder.getFeatureType());

			//For each point of MNT1 check nearest point on mnt2 and get elevation diff	
			FeatureIterator<SimpleFeature> iteratorMNT1 = mnt1.features();
			
			//check if the FeatureCollection contains features
			while(iteratorMNT1.hasNext()){

				SimpleFeature feature = iteratorMNT1.next();
				Geometry geometry = (Geometry) feature.getDefaultGeometry();
				
				LOGGER.debug("Geometry type : "+ geometry.getClass());

				if (geometry instanceof Point){
					Point pt = (Point) geometry;
					pt.getCoordinate().getX();
					pt.getCoordinate().getY();
					pt.getCoordinate().getZ();
					LOGGER.debug("Point X : "+ pt.getCoordinate().getX() + " Y : " + pt.getCoordinate().getY() + " Z : " +pt.getCoordinate().getZ());
				}
				//For each point of MNT1 check nearest point on mnt2 and get elevation diff	
				FeatureIterator<SimpleFeature> iteratorMNT2 = mnt2.features();
				Boolean pointFound = false;

				while(iteratorMNT2.hasNext() && !pointFound){

				}
				iteratorMNT2.close();
						
			}
			iteratorMNT1.close();


		return resultFeatureCollection;
	}

}
