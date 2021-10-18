package jsheets.config;

public final class CamelCase {
  private CamelCase() {}

  @FunctionalInterface
  public interface Case {
    static Case upper() {
      return Character::toUpperCase;
    }

    static Case lower() {
      return Character::toLowerCase;
    }

    static Case same() {
      return character -> character;
    }

    char apply(char character);
  }

  public static String convert(
    String input,
    String separator,
    Case characterCase
  ) {
    return switch (input.length()) {
      case 0 -> input;
      case 1 -> String.valueOf(characterCase.apply(input.charAt(0)));
      default -> convertWithSufficientLength(input, separator, characterCase);
    };
  }

  private static String convertWithSufficientLength(
    String input,
    String separator,
    Case characterCase
  ) {
    var output = new StringBuilder(input.length());
    char firstCharacter = input.charAt(0);
    boolean lastCapitalized = Character.isUpperCase(firstCharacter);
    output.append(characterCase.apply(firstCharacter));
    for (int index = 1; index < input.length(); index++) {
      char character = input.charAt(index);
      boolean capitalized = Character.isUpperCase(character);
      if (capitalized && !lastCapitalized) {
        output.append(separator);
      }
      lastCapitalized = capitalized;
      output.append(characterCase.apply(character));
    }
    return output.toString();
  }
}