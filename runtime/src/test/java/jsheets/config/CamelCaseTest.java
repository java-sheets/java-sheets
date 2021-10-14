package jsheets.config;

import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jsheets.config.CamelCase.Case;

public class CamelCaseTest {
  @Test
  public void emptyReturnsSameInstance() {
    var input = new String(new char[0]); // no interning
    Assertions.assertSame(input, CamelCase.convert(input, "_", Case.upper()));
  }

  @Test
  public void singleCharacterIsConvertedInCase() {
    Assertions.assertEquals("a", CamelCase.convert("A", "_", Case.lower()));
    Assertions.assertEquals("a", CamelCase.convert("a", "_", Case.lower()));
  }

  @Test
  public void testToSnakeCase() {
    Function<String, String> toSnakeCase = input ->
      CamelCase.convert(input, "_", Case.lower());

    Assertions.assertEquals("a_bc", toSnakeCase.apply("aBC"));
    Assertions.assertEquals("g_rpc", toSnakeCase.apply("gRpc"));
    Assertions.assertEquals("g_rpc", toSnakeCase.apply("gRPC"));
    Assertions.assertEquals("my_sql", toSnakeCase.apply("MySql"));
    Assertions.assertEquals("queen", toSnakeCase.apply("QUEEN"));
    Assertions.assertEquals(
      "the_sun_is_burning",
      toSnakeCase.apply("TheSunIsBurning")
    );
    Assertions.assertEquals(
      "simon_and_garfunkel",
      toSnakeCase.apply("simonAndGarfunkel")
    );
  }

  @Test
  public void testToScreamingCase() {
    Function<String, String> toScreamingCase = input ->
      CamelCase.convert(input, "_", Case.upper());

    Assertions.assertEquals("LUKE_KELLY", toScreamingCase.apply("LukeKelly"));
    Assertions.assertEquals("LUKE_KELLY", toScreamingCase.apply("lukeKelly"));
    Assertions.assertEquals("LUKE_KELLY", toScreamingCase.apply("luke_kelly"));
    Assertions.assertEquals("ABC", toScreamingCase.apply("abc"));
  }
}