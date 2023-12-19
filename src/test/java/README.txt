Information about unittests.

Property loading.
For unittest that require the properties to be initialised use this way to load the properties
PropertiesLoader.initProperties(UnitTestUtils.getFile("properties/solrwayback_unittest.properties").getPath());

This will use the property files under test/resources/properties

If you need a unittest with quite different properties, you can create a new property file and load that. Just be sure 
to include unittest in the name of the property.

TODO: more documentation