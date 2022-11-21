package testbridge.utils;

import logisticspipes.utils.tuples.Pair;
import lombok.SneakyThrows;

import java.util.HashMap;
import java.util.Map;

public class ReflectionHelper extends logisticspipes.utils.ReflectionHelper {

  private static final Map<Pair<Class<?>, String>, Object[]> enumCache = new HashMap<>();

  @SuppressWarnings("unchecked")
  @SneakyThrows
  public static <T> T getPrivateEnum(String _enum, String name) {
    Object[] $enum = getEnum(Class.forName(_enum), "constants" + name);
    return (T) $enum;
  }

  @SuppressWarnings("unchecked")
  @SneakyThrows
  public static <T> T getPrivateEValue(String _enum, int id, String name) {
    Object[] $enum = getEnum(Class.forName(_enum), name);
    Object result = $enum[id];
    return (T) result;
  }

  private static Object[] getEnum(Class<?> _enum, String name) {
    Pair<Class<?>, String> key = new Pair<>(_enum, name);
    Object[] $enum = ReflectionHelper.enumCache.get(key);
    if ($enum == null) {
      $enum = _enum.getEnumConstants();
      ReflectionHelper.enumCache.put(key, $enum);
    }
    return $enum;
  }
}
