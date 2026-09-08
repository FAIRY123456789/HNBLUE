// HNBLUE V2.0 public metric extractor for Google Earth Engine Code Editor.
// Run in an authenticated GEE account. Exports compact CSV tables only.

var bamenBay = ee.Geometry.Rectangle([110.62, 19.34, 110.86, 19.62], null, false);
var wenchang = ee.Geometry.Rectangle([110.45, 19.30, 111.10, 20.12], null, false);
var hainan = ee.Geometry.Rectangle([108.55, 18.05, 111.10, 20.18], null, false);

var regions = ee.FeatureCollection([
  ee.Feature(bamenBay, { region_name: 'Wenchang_Bamen_Bay_bbox' }),
  ee.Feature(wenchang, { region_name: 'Wenchang_bbox' }),
  ee.Feature(hainan, { region_name: 'Hainan_bbox' })
]);

function feature(regionName, period, indicator, value, unit, method, isProxy, notes) {
  return ee.Feature(null, {
    source_name: method.split('|')[0],
    source_url: method.split('|')[1],
    source_type: method.split('|')[2],
    region_name: regionName,
    year_or_period: period,
    indicator_name: indicator,
    value: value,
    unit: unit,
    method: method,
    quality_level: 'A',
    is_proxy: isProxy,
    is_simulated: 0,
    notes: notes
  });
}

function reduceMean(image, geom, scale) {
  return image.reduceRegion({
    reducer: ee.Reducer.mean(),
    geometry: geom,
    scale: scale,
    maxPixels: 1e13,
    bestEffort: true
  });
}

function annualModis(year, regionFeature) {
  var geom = regionFeature.geometry();
  var name = ee.String(regionFeature.get('region_name'));
  var img = ee.ImageCollection('MODIS/061/MOD17A3HGF')
    .filterDate(ee.Date.fromYMD(year, 1, 1), ee.Date.fromYMD(year + 1, 1, 1))
    .first()
    .select(['Gpp', 'Npp'])
    .multiply(0.0001);
  var stats = reduceMean(img, geom, 500);
  var method = 'MODIS MOD17A3HGF GPP/NPP|https://developers.google.com/earth-engine/datasets/catalog/MODIS_061_MOD17A3HGF|gee_remote_sensing_product';
  return ee.FeatureCollection([
    feature(name, year, 'GPP_mean', stats.get('Gpp'), 'kg C/m2/year', method, 1, 'Regional product mean; proxy, not flux tower observation.'),
    feature(name, year, 'NPP_mean', stats.get('Npp'), 'kg C/m2/year', method, 1, 'Regional product mean; proxy, not flux tower observation.')
  ]);
}

function annualChirps(year, regionFeature) {
  var geom = regionFeature.geometry();
  var name = ee.String(regionFeature.get('region_name'));
  var img = ee.ImageCollection('UCSB-CHG/CHIRPS/DAILY')
    .filterDate(ee.Date.fromYMD(year, 1, 1), ee.Date.fromYMD(year + 1, 1, 1))
    .sum()
    .rename('precipitation');
  var stats = reduceMean(img, geom, 5566);
  var method = 'CHIRPS Daily Precipitation|https://developers.google.com/earth-engine/datasets/catalog/UCSB-CHG_CHIRPS_DAILY|gee_climate_product';
  return ee.FeatureCollection([
    feature(name, year, 'annual_precipitation_mean', stats.get('precipitation'), 'mm/year', method, 1, 'Regional mean from CHIRPS; climate proxy.')
  ]);
}

function annualEra5(year, regionFeature) {
  var geom = regionFeature.geometry();
  var name = ee.String(regionFeature.get('region_name'));
  var img = ee.ImageCollection('ECMWF/ERA5_LAND/DAILY_AGGR')
    .filterDate(ee.Date.fromYMD(year, 1, 1), ee.Date.fromYMD(year + 1, 1, 1))
    .select('temperature_2m')
    .mean()
    .subtract(273.15)
    .rename('temperature_2m_c');
  var stats = reduceMean(img, geom, 11132);
  var method = 'ERA5-Land Daily Aggregated|https://developers.google.com/earth-engine/datasets/catalog/ECMWF_ERA5_LAND_DAILY_AGGR|gee_reanalysis_product';
  return ee.FeatureCollection([
    feature(name, year, 'air_temperature_2m_mean', stats.get('temperature_2m_c'), 'degree_C', method, 1, 'Regional mean from ERA5-Land; reanalysis proxy.')
  ]);
}

function demStats(regionFeature) {
  var geom = regionFeature.geometry();
  var name = ee.String(regionFeature.get('region_name'));
  var dem = ee.Image('NASA/NASADEM_HGT/001').select('elevation');
  var stats = dem.reduceRegion({
    reducer: ee.Reducer.mean().combine(ee.Reducer.minMax(), '', true),
    geometry: geom,
    scale: 30,
    maxPixels: 1e13,
    bestEffort: true
  });
  var method = 'NASADEM HGT|https://developers.google.com/earth-engine/datasets/catalog/NASA_NASADEM_HGT_001|gee_dem_product';
  return ee.FeatureCollection([
    feature(name, 'static', 'DEM_elevation_mean', stats.get('elevation_mean'), 'm', method, 1, 'BBox regional topographic proxy.'),
    feature(name, 'static', 'DEM_elevation_min', stats.get('elevation_min'), 'm', method, 1, 'BBox regional topographic proxy.'),
    feature(name, 'static', 'DEM_elevation_max', stats.get('elevation_max'), 'm', method, 1, 'BBox regional topographic proxy.')
  ]);
}

var years = ee.List([2015, 2016, 2017, 2018, 2019, 2020]);
var dynamicRows = ee.FeatureCollection(
  regions.map(function (regionFeature) {
    var perYear = ee.FeatureCollection(years.map(function (year) {
      year = ee.Number(year);
      return annualModis(year, regionFeature)
        .merge(annualChirps(year, regionFeature))
        .merge(annualEra5(year, regionFeature));
    })).flatten();
    return perYear.merge(demStats(regionFeature));
  })
).flatten();

print('HNBLUE public metrics preview', dynamicRows.limit(20));

Export.table.toDrive({
  collection: dynamicRows,
  description: 'hnblue_v2_public_metrics_gee',
  fileNamePrefix: 'hnblue_v2_public_metrics_gee',
  fileFormat: 'CSV'
});
