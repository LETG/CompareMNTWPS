package fr.indigeo.wps.mnt;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.geotools.feature.FeatureCollection;
import org.geotools.geojson.geom.GeometryJSON;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import fr.indigeo.wps.mnt.GeoJsonFileUtils;


public class TestWPS {

	private static final Logger LOGGER = Logger.getLogger(TestWPS.class);

	private static final File dataDir = new File("data");
	private static final File mnt1File = new File(dataDir, "mnt1.json");
	private static final File mnt2File = new File(dataDir, "mnt2.json");

	@Test
	public void testAllServices() {
		try {
			// draw radials Test
			FeatureCollection<SimpleFeatureType, SimpleFeature> mnt1 = getFeatureCollections(mnt1File);
			FeatureCollection<SimpleFeatureType, SimpleFeature> mnt2 = getFeatureCollections(mnt2File);

			FeatureCollection<SimpleFeatureType, SimpleFeature> compareMNT = compareMNTTest(mnt1, mnt2);
			getGeoJsonFile(compareMNT, dataDir, "compare");
			LOGGER.info("compare.json est généré dans le dossier data de votre projet ! vous pouvez le visualiser maintenant.");

		} catch (FileNotFoundException e) {
			LOGGER.error("Fichiers introuvables", e);
		} catch (IOException e) {
			LOGGER.error("Erreur entrées sorties", e);
		}
	}

	public static FeatureCollection<SimpleFeatureType, SimpleFeature> compareMNTTest(
			FeatureCollection<SimpleFeatureType, SimpleFeature> mnt1, FeatureCollection<SimpleFeatureType, SimpleFeature> mnt2) {

		return CompareMNTWPS.compareMNT(mnt1, mnt2);
	}

	public static FeatureCollection<SimpleFeatureType, SimpleFeature> getFeatureCollections(File refLineFile)
			throws FileNotFoundException, IOException {

		return GeoJsonFileUtils.geoJsonToFeatureCollection(refLineFile);
	}

	public static void getGeoJsonFile(FeatureCollection<SimpleFeatureType, SimpleFeature> data, File dir, String fileName) throws FileNotFoundException, IOException {
		GeoJsonFileUtils.featureCollectionToGeoJsonFile(data, dir, fileName);
	}

}
