package org.micromanager.display.internal;

import java.awt.Color;
import org.micromanager.PropertyMap;
import org.micromanager.PropertyMaps;
import org.micromanager.UserProfile;
import org.micromanager.display.ChannelDisplaySettings;
import org.micromanager.propertymap.MutablePropertyMapView;

/**
 * Manages the user's last-used channel display settings.
 *
 * @author Mark A. Tsuchida
 */
public class ChannelDisplayDefaults {
   private static final String PROFILE_KEY = "LastUsedChannelDisplaySettings";

   private final UserProfile profile_;

   public ChannelDisplayDefaults(UserProfile profile) {
      profile_ = profile;
   }

   public ChannelDisplaySettings getSettingsForChannel(String channelGroupName, String channelName) {
      PropertyMap groups = profile_.getSettings(ChannelDisplayDefaults.class).getPropertyMap(PROFILE_KEY, null);
      if (groups == null) {
         return null;
      }

      PropertyMap presets = groups.getPropertyMap(channelGroupName, null);
      if (presets == null) {
         return null;
      }

      PropertyMap settings = presets.getPropertyMap(channelName, null);
      if (settings == null) {
         return null;
      }

      return DefaultChannelDisplaySettings.fromPropertyMap(settings);
   }

   public void saveSettingsForChannel(String channelGroupName, String channelName,
         ChannelDisplaySettings settings) {
      MutablePropertyMapView profileSettings = profile_.getSettings(ChannelDisplayDefaults.class);
      PropertyMap empty = PropertyMaps.emptyPropertyMap();
      PropertyMap channelPmap = ((DefaultChannelDisplaySettings) settings).toPropertyMap();

      profileSettings.putPropertyMap(PROFILE_KEY,
            profileSettings.
                  getPropertyMap(PROFILE_KEY, empty).
                  copyBuilder().
                  putPropertyMap(channelGroupName,
                        profileSettings.
                              getPropertyMap(PROFILE_KEY, empty).
                              getPropertyMap(channelGroupName, empty).
                              copyBuilder().
                              putPropertyMap(channelName, channelPmap).
                              build()).
                  build());
   }

   public Color getColorForChannel(String channelGroupName, String channelName, Color defaultColor) {
      ChannelDisplaySettings settings = getSettingsForChannel(channelGroupName, channelName);
      if (settings == null) {
         return defaultColor;
      }
      return settings.getColor();
   }

   public void saveColorForChannel(String channelGroupName, String channelName, Color color) {
      if (color == null) {
         return;
      }

      ChannelDisplaySettings oldSettings = getSettingsForChannel(
            channelGroupName, channelName);

      ChannelDisplaySettings.Builder builder = oldSettings == null ?
            DefaultChannelDisplaySettings.builder() :
            oldSettings.copyBuilder();

      saveSettingsForChannel(channelGroupName, channelName,
            builder.color(color).build());
   }
}