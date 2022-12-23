package nostr.test.json;

import nostr.base.IEvent;
import nostr.base.ITag;
import nostr.base.PublicKey;
import nostr.base.Relay;
import nostr.util.UnsupportedNIPException;
import nostr.event.impl.GenericEvent;
import nostr.event.impl.TextNoteEvent;
import nostr.event.marshaller.impl.EventMarshaller;
import nostr.event.list.TagList;
import nostr.event.marshaller.impl.TagMarshaller;
import nostr.event.tag.DelegationTag;
import nostr.event.tag.EventTag;
import nostr.event.tag.PubKeyTag;
import nostr.event.tag.SubjectTag;
import nostr.id.Wallet;
import nostr.json.parser.JsonParseException;
import nostr.json.JsonValue;
import nostr.json.values.BaseJsonValue;
import nostr.json.values.JsonArrayValue;
import nostr.json.values.JsonNumberValue;
import nostr.json.values.JsonObjectValue;
import nostr.json.types.JsonArrayType;
import nostr.json.types.JsonNumberType;
import nostr.json.types.JsonObjectType;
import nostr.json.types.JsonStringType;
import nostr.json.unmarshaller.impl.JsonArrayUnmarshaller;
import nostr.json.unmarshaller.impl.JsonNumberUnmarshaller;
import nostr.json.unmarshaller.impl.JsonObjectUnmarshaller;
import nostr.json.unmarshaller.impl.JsonStringUnmarshaller;
import nostr.test.EntityFactory;
import java.beans.IntrospectionException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import lombok.extern.java.Log;
import nostr.event.impl.Filters;
import nostr.event.impl.GenericTagQuery;
import nostr.event.marshaller.impl.FiltersMarshaller;
import nostr.event.marshaller.impl.GenericTagQueryMarshaller;
import nostr.json.unmarshaller.impl.JsonExpressionUnmarshaller;
import nostr.json.values.JsonExpression;
import nostr.util.NostrException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author squirrel
 */
@Log
public class JsonTest {

    @Test
    public void testParser() {
        System.out.println("testParser");

        JsonValue<JsonStringType> jsonStr = new JsonStringUnmarshaller("\"34f\"").unmarshall();
        Assertions.assertEquals("34f", jsonStr.getValue().toString());

        JsonValue<JsonNumberType> jsonNum = new JsonNumberUnmarshaller("46").unmarshall();
        Assertions.assertEquals(Integer.parseInt("46"), ((JsonNumberValue) jsonNum).intValue());

        JsonValue<JsonArrayType> jsonArr = new JsonArrayUnmarshaller("[2,\"a\"]").unmarshall();
        Assertions.assertEquals(2, ((JsonArrayValue) jsonArr).length());
        Assertions.assertEquals(2, ((JsonNumberValue) ((JsonArrayValue) jsonArr).get(0)).intValue());
        Assertions.assertEquals("\"a\"", ((JsonArrayValue) jsonArr).get(1).toString());

        jsonArr = new JsonArrayUnmarshaller("[1,2,\"bx\"]").unmarshall();
        Assertions.assertEquals(3, ((JsonArrayValue) jsonArr).length());
        Assertions.assertEquals(1, ((JsonNumberValue) ((JsonArrayValue) jsonArr).get(0)).intValue());
        Assertions.assertEquals(2, ((JsonNumberValue) ((JsonArrayValue) jsonArr).get(1)).intValue());
        Assertions.assertEquals("\"bx\"", ((JsonArrayValue) jsonArr).get(2).toString());

        jsonArr = new JsonArrayUnmarshaller("[2,\"a\",[1,2,\"bx\"]]").unmarshall();
        Assertions.assertEquals(3, ((JsonArrayValue) jsonArr).length());
        Assertions.assertTrue(((BaseJsonValue) ((JsonArrayValue) jsonArr).get(2)).getType() instanceof JsonArrayType);

        jsonArr = new JsonArrayUnmarshaller("[2,\"a\",[1,2,\"bx\"],\"3\"   ,9]").unmarshall();
        Assertions.assertEquals(5, ((JsonArrayValue) jsonArr).length());

        jsonArr = new JsonArrayUnmarshaller("[[\"p\",\"\",\"null\",\"willy\"]]").unmarshall();
        Assertions.assertEquals(1, ((JsonArrayValue) jsonArr).length());

        jsonArr = new JsonArrayUnmarshaller("[[\"p\",\"\",\"null\",\"willy\"],[\"delegation\",\"\",\"whatever\",\"0d321c696337ffa923ea2d8fa40c04a326881063950eec26ce4eb7d06b7e84f78a9dd2a5ea267dfb1fba262568016b3bab533b7269c5b689922b3e157fcccdb9\"]]").unmarshall();
        Assertions.assertEquals(2, ((JsonArrayValue) jsonArr).length());

        JsonValue<JsonObjectType> jsonObj = new JsonObjectUnmarshaller("{    \"a\":2,\"b\":\"a\"}").unmarshall();
        Assertions.assertTrue(((BaseJsonValue<JsonObjectType>) jsonObj).getType() instanceof JsonObjectType);
        JsonValue v = ((JsonObjectValue) jsonObj).get("\"a\"");
        Assertions.assertTrue(((BaseJsonValue) v).getType() instanceof JsonNumberType);
        Assertions.assertEquals(2, ((JsonNumberValue) v).intValue());

        jsonArr = new JsonArrayUnmarshaller("[2,\"a\",[1,2,\"bx\", {\"a\":2,\"b\":\"a\"}]]").unmarshall();
        Assertions.assertEquals(3, ((JsonArrayValue) jsonArr).length());
        v = ((JsonArrayValue) jsonArr).get(2);
        Assertions.assertTrue(((BaseJsonValue) v).getType() instanceof JsonArrayType);
        jsonObj = ((JsonArrayValue) v).get(3);
        Assertions.assertTrue(((BaseJsonValue<JsonObjectType>) jsonObj).getType() instanceof JsonObjectType);

        jsonObj = new JsonObjectUnmarshaller("{\"a\":2,\"b\":\"a\", \"nil\":{}}").unmarshall();
        v = ((JsonObjectValue) jsonObj).get("\"nil\"");
        Assertions.assertTrue(((BaseJsonValue) v).getType() instanceof JsonObjectType);

        Assertions.assertDoesNotThrow(
                () -> {
                    new JsonObjectUnmarshaller(("{\"tags\":[[\"p\",\"f6a04a16b1fb3b4bf40838dacc7f8bd4d46b60d3c9e2a4915877f9a2eac8e323\",\"nostr-java\"]],\"content\":\"Hello Astral, Please replace me!\",\"sig\":\"507b25e85fe42a2c6d2b67bca81f4c04e587448b4c3fc3ff0e6d3ee1ade5bad9758576e3e847d96082d38b389d0febac8d861b3c97534ca9b18afc0c2d4e2a15\",\"id\":\"d5e94446b140631740c7ada24cb8b01a4bb9f6c3c254e6ce61af0ce538968508\",\"kind\":1,\"pubkey\":\"f6a04a16b1fb3b4bf40838dacc7f8bd4d46b60d3c9e2a4915877f9a2eac8e323\",\"created_at\":1671152327}")).unmarshall();
                });
    }

    @Test
    public void testParserFail() {
        System.out.println("testParserFail");

        JsonParseException thrown = Assertions.assertThrows(JsonParseException.class,
                () -> {
                    new JsonStringUnmarshaller("\"34f").unmarshall();
                },
                "Parse error at position 4"
        );
        Assertions.assertNotNull(thrown);

        thrown = Assertions.assertThrows(JsonParseException.class,
                () -> {
                    new JsonObjectUnmarshaller("{    \"a\":2,\"b\"}").unmarshall();
                }
        );
        Assertions.assertNotNull(thrown);

        thrown = Assertions.assertThrows(JsonParseException.class,
                () -> {
                    new JsonObjectUnmarshaller("{\"a\":2,\"b\":\"a\", \"nil\":{}").unmarshall();
                }
        );
        Assertions.assertNotNull(thrown);
    }

    @Test
    public void testMarshalEvent() {
        try {
            System.out.println("testMarshalEvent");

            List<Integer> supportedNips = new ArrayList<>();
            supportedNips.add(1);
            supportedNips.add(5);
            supportedNips.add(16);

            Relay relay = Relay.builder().name("Free Domain").supportedNips(supportedNips).uri("ws://localhost:9999").build();

            PublicKey publicKey = new PublicKey(new byte[]{});

            IEvent event = EntityFactory.Events.createTextNoteEvent(publicKey, "Free Willy!");

            Assertions.assertNotNull(new EventMarshaller(event, relay).marshall());
        } catch (IllegalArgumentException | UnsupportedNIPException ex) {
            Assertions.fail(ex);
        }
    }

    @Test
    public void testMarshalEventWithOts() {
        try {
            System.out.println("testMarshalEventWithOts");

            List<Integer> supportedNips = new ArrayList<>();
            supportedNips.add(1);
            supportedNips.add(5);
            supportedNips.add(16);

            Relay relay = Relay.builder().name("Free Domain").supportedNips(supportedNips).uri("ws://localhost:9999").build();

            PublicKey publicKey = new PublicKey(new byte[]{});

            IEvent event = EntityFactory.Events.createTextNoteEvent(publicKey, "Free Willy!");
            ((GenericEvent) event).setOts(EntityFactory.generateRamdomAlpha(32));

            final String jsonEvent = new EventMarshaller(event, relay).marshall();

            log.log(Level.INFO, "++++++++ jsonEvent: {0}", jsonEvent);

            Assertions.assertNotNull(jsonEvent);

            var jsonValue = new JsonObjectUnmarshaller(jsonEvent).unmarshall();

            Assertions.assertNull(((JsonObjectValue) jsonValue).get("\"ots\""));

        } catch (IllegalArgumentException | UnsupportedNIPException ex) {
            Assertions.fail(ex);
        }
    }

    @Test
    public void testMarshalEventFail() {
        try {
            System.out.println("testMarshalEventFail");

            List<Integer> supportedNips = new ArrayList<>();
            supportedNips.add(5);
            supportedNips.add(16);

            Relay relay = Relay.builder().name("Free Domain").supportedNips(supportedNips).uri("ws://localhost:9999").build();

            PublicKey publicKey = new PublicKey(new byte[]{});

            IEvent event = EntityFactory.Events.createTextNoteEvent(publicKey, "Assange");

            UnsupportedNIPException thrown = Assertions.assertThrows(UnsupportedNIPException.class,
                    () -> {
                        new EventMarshaller(event, relay).marshall();
                    },
                    "This event is not supported. List of relay supported NIP(s): " + relay.printSupportedNips()
            );

            Assertions.assertNotNull(thrown);

        } catch (IllegalArgumentException ex) {
            Assertions.fail(ex);
        }
    }

    @Test
    public void testMarshalTag() {
        try {
            System.out.println("testMarshalTag");

            List<Integer> supportedNips = new ArrayList<>();
            supportedNips.add(1);
            supportedNips.add(5);
            supportedNips.add(16);
            supportedNips.add(26);

            Relay relay = Relay.builder().name("Free Domain").supportedNips(supportedNips).uri("ws://localhost:9999").build();

            PublicKey publicKey = new PublicKey(new byte[]{});

            TagList tags = new TagList();
            tags.add(PubKeyTag.builder().publicKey(publicKey).petName("willy").build());
            final DelegationTag delegationTag = new DelegationTag(publicKey, "whatever");
            Wallet wallet;
            wallet = new Wallet();
            wallet.sign(delegationTag);
            tags.add(delegationTag);

            IEvent event = new TextNoteEvent(publicKey, tags, "Free Willy!");

            final String jsonEvent = new EventMarshaller(event, relay).marshall();

            Assertions.assertNotNull(jsonEvent);

            var jsonValue = ((JsonObjectValue) new JsonObjectUnmarshaller(jsonEvent).unmarshall()).get("\"tags\"");

            var tagsArr = (JsonArrayValue) jsonValue;

            for (int i = 0; i < tagsArr.length(); i++) {
                var t = tagsArr.get(i);
                if (((JsonArrayValue) t).get(0).toString().equals("\"delegation\"")) {
                    Assertions.assertTrue(true);
                }
            }

            Assertions.assertFalse(false);

        } catch (NoSuchAlgorithmException | IntrospectionException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchFieldException | NostrException | IOException ex) {
            Assertions.fail(ex);
        } catch (Exception ex) {
            Assertions.fail(ex);
        }
    }

    @Test
    public void testMarshalSubjectTag() {
        try {
            System.out.println("testMarshalSubjectTag");

            List<Integer> supportedNips = new ArrayList<>();
            supportedNips.add(1);
            supportedNips.add(5);
            supportedNips.add(14);
            supportedNips.add(26);

            Relay relay = Relay.builder().name("Free Domain").supportedNips(supportedNips).uri("ws://localhost:9999").build();

            ITag subjectTag = new SubjectTag("Hello World!");

            var jsonSubjectTag = new TagMarshaller(subjectTag, relay).marshall();

            Assertions.assertNotNull(jsonSubjectTag);

            var jsonValue = ((JsonArrayValue) new JsonArrayUnmarshaller(jsonSubjectTag).unmarshall()).get(0);

            Assertions.assertEquals("\"subject\"", jsonValue.toString());
        } catch (NostrException ex) {
            Assertions.fail(ex);
        }
    }

    @Test
    public void testMarshalEventTag() {
        try {
            System.out.println("testMarshalEventTag");

            List<Integer> supportedNips = new ArrayList<>();
            supportedNips.add(1);
            supportedNips.add(5);
            supportedNips.add(14);
            supportedNips.add(26);

            Relay relay = Relay.builder().name("Free Domain").supportedNips(supportedNips).uri("ws://localhost:9999").build();

            PublicKey publicKey = new PublicKey(new byte[]{});
            GenericEvent relatedEvent = EntityFactory.Events.createTextNoteEvent(publicKey);
            ITag eventTag = new EventTag(relatedEvent);

            var jsonEventTag = new TagMarshaller(eventTag, relay).marshall();

            Assertions.assertNotNull(jsonEventTag);

            var jsonCodeValue = ((JsonArrayValue) new JsonArrayUnmarshaller(jsonEventTag).unmarshall()).get(0);
            var jsonEventIdValue = ((JsonArrayValue) new JsonArrayUnmarshaller(jsonEventTag).unmarshall()).get(1);

            Assertions.assertEquals("\"e\"", jsonCodeValue.toString());
        } catch (NostrException ex) {
            Assertions.fail(ex);
        }
    }

    @Test
    public void testMarshalTagFail() {
        try {
            System.out.println("testMarshalTagFail");

            List<Integer> supportedNips = new ArrayList<>();
            supportedNips.add(100);

            Relay relay = Relay.builder().name("Free Domain").supportedNips(supportedNips).uri("ws://localhost:9999").build();

            PublicKey publicKey = new PublicKey(new byte[]{});

            TagList tags = new TagList();
            tags.add(PubKeyTag.builder().publicKey(publicKey).petName("willy").build());
            final DelegationTag delegationTag = new DelegationTag(publicKey, "whatever");
            Wallet wallet;
            wallet = new Wallet();
            wallet.sign(delegationTag);
            tags.add(delegationTag);

            IEvent event = new TextNoteEvent(publicKey, tags, "Free Willy!");

            UnsupportedNIPException thrown = Assertions.assertThrows(UnsupportedNIPException.class,
                    () -> {
                        new EventMarshaller(event, relay).marshall();
                    }
            );

            Assertions.assertNotNull(thrown);

        } catch (NoSuchAlgorithmException | IntrospectionException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchFieldException | NostrException | IOException ex) {
            Assertions.fail(ex);
        } catch (Exception ex) {
            Assertions.fail(ex);
        }
    }

    @Test
    public void testGenericTagQueryMarshaller() {
        try {
            System.out.println("testGenericTagQuery");

            List<Integer> supportedNips = new ArrayList<>();
            supportedNips.add(1);
            supportedNips.add(5);
            supportedNips.add(14);
            supportedNips.add(12);

            Relay relay = Relay.builder().name("Free Domain").supportedNips(supportedNips).uri("ws://localhost:9999").build();

            GenericTagQuery gtq = EntityFactory.Events.createGenericTagQuery();

            GenericTagQueryMarshaller gtqm = new GenericTagQueryMarshaller(gtq, relay);

            String strExpr = gtqm.marshall();

            JsonValue vexpr = new JsonExpressionUnmarshaller(strExpr).unmarshall();

            Assertions.assertTrue(vexpr instanceof JsonExpression);

            JsonExpression expr = (JsonExpression) vexpr;

            String variable = "\"#" + gtq.getTagName().toString() + "\"";
            Assertions.assertEquals(variable, expr.getVariable());

            var jsonValue = expr.getJsonValue();
            Assertions.assertTrue(jsonValue instanceof JsonArrayValue);

            var jsonArrValue = (JsonArrayValue) jsonValue;
            for (int i = 0; i < jsonArrValue.length(); i++) {
                var v = jsonArrValue.get(i).getValue().toString();
                Assertions.assertTrue(gtq.getValue().contains(v));
            }

        } catch (NostrException ex) {
            Assertions.fail(ex);
        }
    }

    @Test
    public void testFilters() {
        try {
            System.out.println("testFilters");

            List<Integer> supportedNips = new ArrayList<>();
            supportedNips.add(1);
            supportedNips.add(5);
            supportedNips.add(14);
            supportedNips.add(12);

            Relay relay = Relay.builder().name("Free Domain").supportedNips(supportedNips).uri("ws://localhost:9999").build();

            PublicKey publicKey = new PublicKey(new byte[]{});

            Filters filters = EntityFactory.Events.createFilters(publicKey);

            var fm = new FiltersMarshaller(filters, relay);
            var strJson = fm.marshall();
            
            System.out.println("@@@ " + strJson);

            JsonValue<JsonObjectType> fObj = new JsonObjectUnmarshaller(strJson).unmarshall();

            JsonObjectValue obj = (JsonObjectValue) fObj;

            JsonValue ids = obj.get("\"ids\"");
            Assertions.assertNotNull(ids);
            Assertions.assertTrue(ids instanceof JsonArrayValue);
            Assertions.assertEquals(2, ((JsonArrayValue) ids).length());

            JsonValue e = obj.get("\"#e\"");
            Assertions.assertNotNull(e);
            Assertions.assertTrue(e instanceof JsonArrayValue);
            Assertions.assertEquals(1, ((JsonArrayValue) e).length());

            var gtql = filters.getGenericTagQueryList();
            Assertions.assertEquals(1, gtql.size());

            var c = gtql.getList().get(0).getTagName();
            var variable = "\"#" + c.toString() + "\"";
            Assertions.assertNotNull(obj.get(variable));

        } catch (UnsupportedNIPException ex) {
            Assertions.fail(ex);
        }
    }
}
