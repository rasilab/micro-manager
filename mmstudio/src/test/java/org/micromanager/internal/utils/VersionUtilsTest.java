package org.micromanager.internal.utils;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VersionUtilsTest {
   @Test
   public void testVersions() {
      String[] olds = new String[] {"10", "9", "9", "17", "1.2", "1.2.3"};
      String[] news = new String[] {"11.0.0", "10", "10.0.0", "18.1.2",
         "1.2.3", "1.2.4"};
      for (int i = 0; i < olds.length; ++i) {
         assertTrue(VersionUtils.isOlderVersion(olds[i], news[i]));
         assertFalse(VersionUtils.isOlderVersion(news[i], olds[i]));
      }
   }
}
