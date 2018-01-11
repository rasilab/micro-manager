///////////////////////////////////////////////////////////////////////////////
//PROJECT:       Micro-Manager
//SUBSYSTEM:     mmstudio
//-----------------------------------------------------------------------------
//
// AUTHOR:       Chris Weisiger, 2015
//
// COPYRIGHT:    University of California, San Francisco, 2006-2015
//
// LICENSE:      This file is distributed under the BSD license.
//               License text is included with the source distribution.
//
//               This file is distributed in the hope that it will be useful,
//               but WITHOUT ANY WARRANTY; without even the implied warranty
//               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.
//

package org.micromanager.display.internal;

import java.awt.Color;
import org.micromanager.internal.utils.UserProfileStaticInterface;

/**
 * Stub left for use by acqEngine (temporary).
 * <p>
 * @deprecated Do not use in new code.
 */
@Deprecated
public final class RememberedChannelSettings {
   @Deprecated
   public static Color getColorForChannel(String channelName,
         String channelGroup, Color defaultColor) {
      ChannelDisplayDefaults defaults = new ChannelDisplayDefaults(UserProfileStaticInterface.getInstance());
      return defaults.getColorForChannel(channelGroup, channelName,
            defaultColor);
   }
}