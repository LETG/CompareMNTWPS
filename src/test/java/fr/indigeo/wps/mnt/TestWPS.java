package fr.indigeo.wps.mnt;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.FeatureCollection;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;



public class TestWPS {

	private static final Logger LOGGER = Logger.getLogger(TestWPS.class);

	private static final File dataDir = new File("data");
	private static final File mnt1File = new File(dataDir, "mnt1.json");
	private static final File mnt2File = new File(dataDir, "mnt2.json");

	@Test
	public void testCloudComparaison() {
		try {
			//  compare cloud
			FeatureCollection<SimpleFeatureType, SimpleFeature> mnt1 = getFeatureCollections(mnt1File);
			FeatureCollection<SimpleFeatureType, SimpleFeature> mnt2 = getFeatureCollections(mnt2File);

			FeatureCollection<SimpleFeatureType, SimpleFeature> compareMNT = CompareMNTWPS.compareMNT(mnt1, mnt2);
			getGeoJsonFile(compareMNT, dataDir, "compare");
			LOGGER.info("compare.json est généré dans le dossier data de votre projet ! vous pouvez le visualiser maintenant.");

		} catch (FileNotFoundException e) {
			LOGGER.error("Fichiers introuvables", e);
		} catch (IOException e) {
			LOGGER.error("Erreur entrées sorties", e);
		}
	}

	@Test
	public void testRasterComparaison() {
		try {
			LOGGER.info("==================== START PROCESS ====================");
			//  compare cloud
			String codeSite = "BOUTRO";
			String initDate = "20061128";
			String compareDate = "20070307";

			FeatureCollection<SimpleFeatureType, SimpleFeature> compareMNT = CompareMNTWPS.compareRasterMNT(codeSite,
					initDate, compareDate, 0.0);
			getGeoJsonFile(compareMNT, dataDir, "compare");
			LOGGER.info(
					"compare.json est généré dans le dossier data de votre projet ! vous pouvez le visualiser maintenant.");
			LOGGER.info("==================== END PROCESS ====================");

		} catch (FileNotFoundException e) {
			LOGGER.error("Fichiers introuvables", e);
		} catch (IOException e) {
			LOGGER.error("Erreur entrées sorties", e);
		}
	}

	@Test
	public void testRasterComparaisonTiffByWps() {
		// this test will use mnt tiff generated by MNT WPS
		String codeSite = "BOUTRO";
		String initDate = "20061128";
		String compareDate = "20070307";
		testRasterComparaisonTiff(codeSite, initDate, compareDate, 0.0);
	}

	@Test
	public void testRasterComparaisonTiffBySaga() {
		// this test will use mnt tiff generated by SAGA tools
		String codeSite = "SAGA_VOUGOT";
		String initDate = "20050701";
		String compareDate = "20090101";
		testRasterComparaisonTiff(codeSite, initDate, compareDate, 0.0);
	}
	

	public static void testRasterComparaisonTiff(String codeSite, String initDate, String compareDate, double interval) {
		try {
			LOGGER.info("==================== START PROCESS ====================");

			GridCoverage2D result = CompareMNTWPS.compareMNToTiff(codeSite,
					initDate, compareDate, interval);
			// save file
			String filePath = CompareMNTWPS.getTiffOutputPath(codeSite, initDate, compareDate);
			File file = new File(filePath);
			GeoTiffWriter writer = new GeoTiffWriter(file);
			writer.write(result, null);
			writer.dispose();
			
			LOGGER.info(
					"compare.json est généré dans le dossier data de votre projet ! vous pouvez le visualiser maintenant.");
			LOGGER.info("==================== END PROCESS ====================");

		} catch (FileNotFoundException e) {
			LOGGER.error("Fichiers introuvables", e);
		} catch (IOException e) {
			LOGGER.error("Erreur entrées sorties", e);
		}
	}

	public static FeatureCollection<SimpleFeatureType, SimpleFeature> getFeatureCollections(File refLineFile)
			throws FileNotFoundException, IOException {

		return GeoJsonFileUtils.geoJsonToFeatureCollection(refLineFile);
	}

	public static void getGeoJsonFile(FeatureCollection<SimpleFeatureType, SimpleFeature> data, File dir,
			String fileName) throws FileNotFoundException, IOException {
		GeoJsonFileUtils.featureCollectionToGeoJsonFile(data, dir, fileName);
	}
}
