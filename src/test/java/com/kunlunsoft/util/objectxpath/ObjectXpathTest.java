package com.kunlunsoft.util.objectxpath;

import org.junit.jupiter.api.Test;
import com.kunlunsoft.util.objectxpath.exception.InvalidXPathExpressionError;
import com.kunlunsoft.util.objectxpath.object.Characteristic;
import com.kunlunsoft.util.objectxpath.object.Engine;
import com.kunlunsoft.util.objectxpath.object.Gear;
import com.kunlunsoft.util.objectxpath.object.Sedan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static com.kunlunsoft.util.objectxpath.Lists.arrayListOf;

@SuppressWarnings("unchecked")
class ObjectXpathTest {
    final ObjectXpath oxp = new ObjectXpath();
    Sedan sedan;
    Object result;
    List list;

    {
        sedan = new Sedan();
        sedan.trunkSize = 10.5;
        sedan.serialNumber = 1111;

        {
            sedan.details = new HashMap<>();
            sedan.details.put("d1", "details1");
            sedan.details.put("d2", "details2");
        }

        {
            Gear gear1 = new Gear();
            gear1.name = "gear_1";
            gear1.setId("111");
            gear1.weight = 1.2;
            gear1.characteristics = new ArrayList<>();
            gear1.characteristics.add(new Characteristic("11", "model:1"));
            gear1.characteristics.add(new Characteristic("22", "size:1"));
            gear1.characteristics.add(null);

            Gear gear2 = new Gear();
            gear2.name = "gear_2";
            gear2.setId("112");
            gear2.weight = 1.5;
            gear2.characteristics = new ArrayList<>();

            Gear gear3 = new Gear();
            gear3.name = "gear_3";
            gear3.setId("113");
            gear3.weight = 1.2;
            gear3.characteristics = new ArrayList<>();
            gear3.characteristics.add(new Characteristic("13", "model:12"));
            gear3.characteristics.add(new Characteristic("14", "size:1"));

            List<Gear> gears = new ArrayList<>();
            gears.add(gear1);
            gears.add(gear2);
            gears.add(gear3);

            sedan.setGears(gears);
        }

        {
            sedan.countryCodes = new ArrayList<>();
            sedan.countryCodes.add(55);
            sedan.countryCodes.add(77);

            sedan.engine = new Engine();
            sedan.engine.setVolume(2.0);

            sedan.engine.valve = new Gear();
            sedan.engine.valve.setId("15");
            sedan.engine.valve.name = "valve";

            Gear gear = new Gear();
            gear.setId("311");
            gear.name = "gear_31";
            gear.weight = 11;
            gear.characteristics = new ArrayList<>();
            gear.characteristics.add(new Characteristic("331", "color:red"));
            gear.characteristics.add(new Characteristic("332", "sn:22"));
            gear.characteristics.add(new Characteristic("333", "year:2000"));

            sedan.engine.gears = arrayListOf(gear);
        }
    }

    //todo map of map? :O
    //todo map with weird keys
    //todo map with number key
    //todo collection as root

    @Test
    void test() {
    }

    @Test
    void listAsRoot() {
        List<String> list = arrayListOf("h1", "h2");
        this.result = oxp.process("/", list);
        assertEquals(list, result);
        this.list = (List) oxp.process("/*", list);
        assertEquals(2, this.list.size());
        assertTrue(this.list.stream().anyMatch(item -> item.equals("h1")));
        assertTrue(this.list.stream().anyMatch(item -> item.equals("h2")));
        this.list = (List) oxp.process("//*", list);
        assertEquals(2, this.list.size());
        assertTrue(this.list.stream().anyMatch(item -> item.equals("h1")));
        assertTrue(this.list.stream().anyMatch(item -> item.equals("h2")));
    }

    @Test
    void primitiveAsRoot() {
        result = oxp.process("/", 25);
        assertEquals(25, result);
        result = oxp.process("/", "25");
        assertEquals("25", result);
        result = oxp.process("/", 22.6);
        assertEquals(22.6, result);
        result = oxp.process("/value", 25);
        assertNull(result);
        result = oxp.process("/*", 25);
        assertNull(result);
    }

    @Test
    void mapAsRoot() {
        Map<String, String> map = new HashMap<>();
        map.put("record-11", "details-11");
        map.put("record-12", "details-12");
        result = oxp.process("/record-11", map);
        assertEquals("details-11", result);
        result = oxp.process("/record-12", map);
        assertEquals("details-12", result);
        list = (List) oxp.process("/*", map);
        assertEquals(2, list.size());
        assertTrue(list.stream().anyMatch(item -> item.equals("details-11")));
        assertTrue(list.stream().anyMatch(item -> item.equals("details-12")));
        list = (List) oxp.process("//*", map);
        assertEquals(2, list.size());
        assertTrue(list.stream().anyMatch(item -> item.equals("details-11")));
        assertTrue(list.stream().anyMatch(item -> item.equals("details-12")));
    }

    @Test
    void mapWithComplexStringKeys() {
        sedan.details.put("detail-21", "details");
        result = oxp.process("/details/detail-21", sedan);
        assertEquals("details", result);
        result = oxp.process("/details/d1", sedan);
        assertEquals(sedan.details.get("d1"), result);
        sedan.details.clear();
        result = oxp.process("/details/detail-21", sedan);
        assertNull(result);
    }

    @Test
    void emptyMap() {
        sedan.details.clear();
        result = oxp.process("/details/detail1", sedan);
        assertNull(result);
    }

    @Test
    void process() {
        result = processXpath("//engine/@volume");
        assertEquals(2.0D, result);

        result = processXpath("/gears[0]/characteristics[1]");
        assertEquals(sedan.getGears().get(0).characteristics.get(1), result);

        result = processXpath("/gears[@id='111']/characteristics");
        assertNull(result);

        result = processXpath("/gears/gear[@id='111']/characteristics");
        assertEquals(sedan.getGears().get(0).characteristics, result);

        result = processXpath("/gears[0]//characteristics[0]/@details");
        assertEquals(sedan.getGears().get(0).characteristics.get(0).details, result);

        result = processXpath("//gears[0]//characteristics[1]/@details");
        assertNull(result);

        result = processXpath("/countryCodes[0]");
        assertEquals(sedan.countryCodes.get(0), result);

        result = processXpath("/countryCodes[1]");
        assertEquals(sedan.countryCodes.get(1), result);

        result = processXpath("/gears[1]//characteristics");
        assertEquals(sedan.getGears().get(1).characteristics, result);

        result = processXpath("/engine");
        assertEquals(sedan.engine, result);

        result = processXpath("/engine[@volume='2.0']");
        assertEquals(sedan.engine, result);

        result = processXpath("/engine/characteristics");
        assertNull(result);

        list = (List) processXpath("/engine//characteristics");
        assertEquals(3, list.size());
        assertEquals(sedan.engine.gears.get(0).characteristics.get(0), list.get(0));
        assertEquals(sedan.engine.gears.get(0).characteristics.get(1), list.get(1));
        assertEquals(sedan.engine.gears.get(0).characteristics.get(2), list.get(2));

        result = processXpath("//valve");
        assertEquals(sedan.engine.valve, result);

        result = processXpath("//valve/@name");
        assertEquals(sedan.engine.valve.name, result);

        result = processXpath("/");
        assertEquals(sedan, result);

        list = (List) processXpath("//@weight");
        assertEquals(5, list.size());

        //multiple items
        {
            list = (List) processXpath("//characteristic[@details='size:1']");
            assertEquals(2, list.size());
            assertEquals(sedan.getGears().get(0).characteristics.get(1), list.get(0));
            assertEquals(sedan.getGears().get(2).characteristics.get(1), list.get(1));

            list = (List) processXpath("/gears//gear");
            assertEquals(3, list.size());
            assertEquals(sedan.getGears().get(0), list.get(0));
            assertEquals(sedan.getGears().get(1), list.get(1));
            assertEquals(sedan.getGears().get(2), list.get(2));

            list = (List) processXpath("//characteristic");
            assertEquals(7, list.size());
            assertEquals(sedan.getGears().get(0).characteristics.get(0), list.get(0));
            assertEquals(sedan.getGears().get(0).characteristics.get(1), list.get(1));
            assertEquals(sedan.getGears().get(2).characteristics.get(0), list.get(2));
            assertEquals(sedan.getGears().get(2).characteristics.get(1), list.get(3));
            assertEquals(sedan.engine.gears.get(0).characteristics.get(0), list.get(4));
            assertEquals(sedan.engine.gears.get(0).characteristics.get(1), list.get(5));
            assertEquals(sedan.engine.gears.get(0).characteristics.get(2), list.get(6));
        }

        //any node *
        {
            list = (List) processXpath("/*");
            assertEquals(4, list.size());
            assertEquals(sedan.details, list.get(3));
            assertEquals(sedan.getGears(), list.get(0));
            assertEquals(sedan.countryCodes, list.get(1));
            assertEquals(sedan.engine, list.get(2));

            list = (List) processXpath("//*");
            assertEquals(25, list.size());

            result = processXpath("//*[@details='sn:22']");
            assertEquals(sedan.engine.gears.get(0).characteristics.get(1), result);

            list = (List) processXpath("/engine/*");
            assertEquals(2, list.size());
            assertTrue(list.stream().anyMatch(o -> sedan.engine.valve.equals(o)));
            assertTrue(list.stream().anyMatch(o -> sedan.engine.gears.equals(o)));
        }

        //maps
        {
            result = processXpath("/details/d1");
            assertEquals(sedan.details.get("d1"), result);

            result = processXpath("/details/d2");
            assertEquals(sedan.details.get("d2"), result);

            result = processXpath("/details/d3");
            assertNull(result);
        }

        //.. parent node
        {
            result = processXpath("/gears/..");
            assertEquals(sedan, result);

            result = processXpath("/gears/../gears");
            assertEquals(sedan.getGears(), result);

            result = processXpath("/..");
            assertNull(result);

            result = processXpath("//*[@id='22']/../..");
            assertEquals(sedan.getGears().get(0), result);
        }
    }

    @Test
    void nullObject() {
        assertThrows(IllegalArgumentException.class, () -> oxp.process("/", null));
    }

    @Test
    void processNegative() {
        assertThrows(NullPointerException.class, () -> checkInvalidXpath(null));
        checkInvalidXpath("/engine/");
        checkInvalidXpath("///");
        checkInvalidXpath("/[]");
    }

    Object processXpath(String xPath) {
        return oxp.process(xPath, sedan);
    }

    void checkInvalidXpath(String xPath) {
        try {
            processXpath(xPath);
        } catch (InvalidXPathExpressionError e) {
            return;
        }
        throw new AssertionError(xPath + " was treated like valid XPath expression");
    }
}