package json;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * A generic type adapter for Gson, which deals with structures, where
 * a statically typed element can have dynamic sub-types. The solution is a
 * simple adaptation of <a href="https://github.com/mperdikeas/json-polymorphism"
 * >https://github.com/mperdikeas/json-polymorphism</a>, which can be used
 * in a generic way. The type parameter <code>E</code> refers to the top of
 * the class hierarchy resp. to the static type, which is dynamically sub-typed
 * in the structure.
 *
 * @author Menelaos Perdikeas, https://github.com/mperdikeas
 * @author Ekkart Kindler, ekki@dtu.dk
 *
 * @param <E> The top of the class hierarchy
 * @author Malte B. Kristensen,s185039@student.dtu.dk
 */
public class Adapter<E> implements JsonSerializer<E>, JsonDeserializer<E>{

    private static final String CLASSNAME = "CLASSNAME";
    private static final String INSTANCE  = "INSTANCE";

    @Override
    public JsonElement serialize(E src, Type typeOfSrc,
                                 JsonSerializationContext context) {

        JsonObject retValue = new JsonObject();
        String className = src.getClass().getName();
        retValue.addProperty(CLASSNAME, className);
        JsonElement elem = context.serialize(src);
        retValue.add(INSTANCE, elem);
        return retValue;
    }

    @Override
    public E deserialize(JsonElement json, Type typeOfT,
                         JsonDeserializationContext context) throws JsonParseException  {
        JsonObject jsonObject = json.getAsJsonObject();
        JsonPrimitive prim = (JsonPrimitive) jsonObject.get(CLASSNAME);
        String className = prim.getAsString();

        Class<?> klass = null;
        try {
            klass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new JsonParseException(e.getMessage());
        }
        return context.deserialize(jsonObject.get(INSTANCE), klass);
    }
}