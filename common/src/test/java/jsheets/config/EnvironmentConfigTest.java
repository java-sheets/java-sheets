package jsheets.config;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EnvironmentConfigTest {
  @Test
  public void testLookup() {
    var config = new EnvironmentConfig(
      "MIRIAM_",
      Map.of(
        "MIRIAM_TASTE_FAVOURITE_SONG", "A Poem on the Underground Wall",
        "MIRIAM_TASTE_FAVOURITE_COLOR", "Teal",
        "MIRIAM_TASTE_WORST_MOVIE", "Titanic",
        "MIRIAM_PHONE_MAKE", "jMobile"
      )
    );
    Assertions.assertEquals(
      "A Poem on the Underground Wall",
      config.lookup(Config.Key.ofString("taste.favouriteSong")).require()
    );
    Assertions.assertEquals(
      "jMobile",
      config.lookup(Config.Key.ofString("phone.make")).require()
    );
    Assertions.assertFalse(
      config.lookup(Config.Key.ofString("phone.color")).exists()
    );
    Assertions.assertTrue(
      config.lookup(Config.Key.ofString("taste.worstMovie")).exists()
    );
    Assertions.assertFalse(
      config.lookup(Config.Key.ofString("miriam.taste.worstMovie")).exists()
    );
  }

  @Test
  public void testTranslateKey() {
    Assertions.assertEquals(
      "OPINION_THE_BEST_SONG",
      EnvironmentConfig.translateKey("opinion.theBestSong")
    );
    Assertions.assertEquals(
      "ARTIST_NINA_SIMONE_BLUES",
      EnvironmentConfig.translateKey("artist.nina_simone.blues")
    );
    Assertions.assertEquals(
      "PLAYBACK_SONG_NAME",
      EnvironmentConfig.translateKey("playback.song.name")
    );
    Assertions.assertEquals(
      "PLAYBACK_SONG_NAME",
      EnvironmentConfig.translateKey("playback.songName")
    );
    Assertions.assertEquals(
      "PLAYBACK_SONG_NAME",
      EnvironmentConfig.translateKey("playbackSongName")
    );
  }
}