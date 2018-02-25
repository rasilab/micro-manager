package org.micromanager.display.inspector.internal.panels.intensity;

import com.bulenkov.iconloader.IconLoader;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.IntStream;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.event.EventListenerSupport;

/**
 *
 * @author mark
 */
public final class RGBComponentSelector extends JPanel {
   private static final String INDEX_PROPERTY = "index";
   private static final List<String> NAMES = ImmutableList.of(
         "red", "green", "blue");
   private static final String ACTIVE_ICON_FORMAT = "/org/micromanager/icons/rgb_%s.png";
   private static final String INACTIVE_ICON_FORMAT = "/org/micromanager/icons/rgb_%s_blank.png";

   public static interface Listener {
      void rgbComponentSelectorDidChangeSelection(int componentIndex);
   }

   private final EventListenerSupport<Listener> listeners_ =
         new EventListenerSupport<>(Listener.class,
               this.getClass().getClassLoader());

   private final List<JToggleButton> buttons_ = IntStream.range(0, 3).
         mapToObj(i -> {
            JToggleButton button = new JToggleButton();
            button.putClientProperty(INDEX_PROPERTY, i);
            return button;
         }).
         collect(ImmutableList.toImmutableList());

   private int selectedComponent_ = 0;

   public static RGBComponentSelector create() {
      return new RGBComponentSelector();
   }

   private RGBComponentSelector() {
      buttons_.forEach(button -> {
         int i = (Integer) button.getClientProperty(INDEX_PROPERTY);
         String name = NAMES.get(i);
         button.setIcon(IconLoader.getIcon(String.format(INACTIVE_ICON_FORMAT, name)));
         button.setSelectedIcon(IconLoader.getIcon(String.format(ACTIVE_ICON_FORMAT, name)));
         button.setBorder(BorderFactory.createEmptyBorder());
         button.setBorderPainted(false);
         button.setOpaque(true);
         button.addActionListener(event -> handleSelect(i));
      });

      setLayout(new MigLayout(new LC().fill().insets("0").gridGap("0", "0")));
      buttons_.forEach(button -> add(button, new CC()));

      buttons_.get(selectedComponent_).setSelected(true);
   }

   int getSelectedComponent() {
      return selectedComponent_;
   }

   void setSelectedComponent(int index) {
      selectedComponent_ = index;
      buttons_.forEach(button -> {
         button.setSelected(button.getClientProperty(INDEX_PROPERTY).equals(index));
      });
   }

   private void handleSelect(int index) {
      setSelectedComponent(index);
      listeners_.fire().rgbComponentSelectorDidChangeSelection(index);
   }

   void addListener(Listener listener) {
      listeners_.addListener(listener);
   }

   void removeListener(Listener listener) {
      listeners_.removeListener(listener);
   }
}