///////////////////////////////////////////////////////////////////////////////
//PROJECT:       Micro-Manager
//SUBSYSTEM:     mmstudio
//-----------------------------------------------------------------------------
//
// AUTHOR:       Chris Weisiger, cweisiger@msg.ucsf.edu, June 2015
//
// COPYRIGHT:    University of California, San Francisco, 2006
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
// CVS:          $Id$

package org.micromanager.display.internal.gearmenu;

import com.bulenkov.iconloader.IconLoader;
import ij.CompositeImage;
import ij.ImagePlus;
import java.awt.Color;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.miginfocom.layout.CC;
import net.miginfocom.swing.MigLayout;
import org.micromanager.data.Coords;
import org.micromanager.data.DataProvider;
import org.micromanager.data.Datastore;
import org.micromanager.display.DisplayWindow;
import org.micromanager.display.ImageExporter;
import org.micromanager.internal.utils.UserProfileStaticInterface;
import org.micromanager.internal.utils.MMDialog;
import org.micromanager.internal.utils.ReportingUtils;
import org.micromanager.internal.utils.WaitDialog;


/**
 * This dialog provides an interface for exporting (a portion of) a dataset
 * to an image sequence, complete with all MicroManager overlays.
 */
public final class ExportMovieDlg extends MMDialog {
   private static final Icon ADD_ICON =
               IconLoader.getIcon("/org/micromanager/icons/plus_green.png");
   private static final Icon DELETE_ICON =
               IconLoader.getIcon("/org/micromanager/icons/minus.png");
   private static final String DEFAULT_EXPORT_FORMAT = "default format to use for exporting image sequences";
   private static final String DEFAULT_FILENAME_PREFIX = "default prefix to use for files when exporting image sequences";

   private static final String FORMAT_PNG = "PNG";
   private static final String FORMAT_JPEG = "JPEG";
   private static final String FORMAT_IMAGEJ = "ImageJ RGB Stack";
   private static final String[] OUTPUT_FORMATS = {
      FORMAT_IMAGEJ,
      FORMAT_PNG,
      FORMAT_JPEG,
   };

   /**
    * A set of controls for selecting a range of values for a single axis of
    * the dataset. A recursive structure; each panel can contain a child
    * panel, to represent the nested-loop nature of the export process.
    */
   public static class AxisPanel extends JPanel {
      private final DataProvider store_;
      private JComboBox axisSelector_;
      private JSpinner minSpinner_;
      private SpinnerNumberModel minModel_ = null;
      private JSpinner maxSpinner_;
      private SpinnerNumberModel maxModel_ = null;
      private JButton addButton_;
      private AxisPanel child_;
      private String oldAxis_;
      // Hacky method of coping with action events we don't care about.
      private boolean amInSetAxis_ = false;

      public AxisPanel(DisplayWindow display, final ExportMovieDlg parent) {
         super(new MigLayout("flowx"));
         super.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
         store_ = display.getDataProvider();
         List<String> axes = new ArrayList<String>(
               parent.getNonZeroAxes());
         Collections.sort(axes);
         axisSelector_ = new JComboBox(axes.toArray(new String[] {}));
         axisSelector_.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               String newAxis = (String) axisSelector_.getSelectedItem();
               if (!amInSetAxis_) {
                  // this event was directly caused by the user.
                  parent.changeAxis(oldAxis_, newAxis);
                  setAxis(newAxis);
               }
            }
         });
         minSpinner_ = new JSpinner();
         minSpinner_.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
               // Ensure that the end point can't go below the start point.
               int newMin = (Integer) minSpinner_.getValue();
               maxModel_.setMinimum(newMin);
            }
         });
         maxSpinner_ = new JSpinner();
         maxSpinner_.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
               // Ensure that the start point can't come after the end point.
               int newMax = (Integer) maxSpinner_.getValue();
               minModel_.setMaximum(newMax);
            }
         });

         final AxisPanel localThis = this;
         final String ADD_BUTTON_NAME = "Add Inner Series";
         final String REMOVE_BUTTON_NAME = "Remove Inner Series";
         addButton_ = new JButton(ADD_BUTTON_NAME, ADD_ICON);
         addButton_.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               if (addButton_.getText().equals(ADD_BUTTON_NAME)) {
                  // Add a panel "under" us.
                  child_ = parent.createAxisPanel();
                  add(child_, "newline, span");
                  addButton_.setText(REMOVE_BUTTON_NAME);
                  addButton_.setIcon(DELETE_ICON);
                  parent.pack();
               }
               else {
                  remove(child_);
                  parent.deleteFollowing(localThis);
                  child_ = null;
                  addButton_.setText(ADD_BUTTON_NAME);
                  addButton_.setIcon(ADD_ICON);
               }
            }
         });

         int spinnerWidth = 10 * minSpinner_.getFontMetrics(
               minSpinner_.getFont()).charWidth('0');

         super.add(new JLabel("Export Series Along: "));
         super.add(axisSelector_);
         super.add(new JLabel(" From: "));
         super.add(minSpinner_, new CC().minWidth(Integer.toString(spinnerWidth)));
         super.add(new JLabel(" To: "));
         super.add(maxSpinner_, new CC().minWidth(Integer.toString(spinnerWidth)));
         // Only show the add button if there's an unused axis we can add.
         // HACK: the 1 remaining is us, because we're still in our
         // constructor.
         if (parent.getNumSpareAxes() > 1) {
            super.add(addButton_);
         }
      }

      public void setAxis(String axis) {
         int axisLen = store_.getAxisLength(axis);
         String curAxis = (String) axisSelector_.getSelectedItem();
         if (curAxis.equals(axis) && minModel_ != null) {
            // Already set properly and spinner models exist.
            return;
         }
         amInSetAxis_ = true;
         oldAxis_ = axis;
         if (minModel_ == null) {
            // Create the spinner models now.
            // Remember our indices here are 1-indexed.
            minModel_ = new SpinnerNumberModel(1, 1, axisLen - 1, 1);
            maxModel_ = new SpinnerNumberModel(axisLen, 2, axisLen, 1);
            minSpinner_.setModel(minModel_);
            maxSpinner_.setModel(maxModel_);
         }
         else {
            // Update their maxima according to the new axis.
            minModel_.setMaximum(axisLen - 1);
            maxModel_.setMaximum(axisLen);
            minModel_.setValue(1);
            maxModel_.setValue(axisLen);
         }
         axisSelector_.setSelectedItem(axis);
         amInSetAxis_ = false;
      }

      public String getAxis() {
         return (String) axisSelector_.getSelectedItem();
      }

      /**
       * Apply our configuration to the provided ImageExporter, and recurse
       * if appropriate to any contained AxisPanel.
       * @param builder
       */
      public void configureExporter(ImageExporter.Builder builder) {
         // Correct for the 1-indexed GUI values, since coords are 0-indexed.
         int minVal = (Integer) (minSpinner_.getValue()) - 1;
         int maxVal = (Integer) (maxSpinner_.getValue()) - 1;
         builder.loop(getAxis(), minVal, maxVal + 1);
         if (child_ != null) {
            child_.configureExporter(builder);
         }
      }

      @Override
      public String toString() {
         return "<AxisPanel for axis " + getAxis() + ">";
      }
   }

   private final DisplayWindow display_;
   private final DataProvider provider_;
   private final ArrayList<AxisPanel> axisPanels_;
   private final JPanel contentsPanel_;
   private JComboBox outputFormatSelector_;
   private JLabel prefixLabel_;
   private JTextField prefixText_;
   private JPanel jpegPanel_;
   private JSpinner jpegQualitySpinner_;

   /**
    * Show the dialog.
    * @param display display showing the data to be exported
    */
   public ExportMovieDlg(DisplayWindow display) {
      super();
      // position the export dialog over the center of the display:
      Window dw = display.getWindow();
      int centerX = dw.getX() + dw.getWidth() / 2;
      int centerY = dw.getY() + dw.getHeight() / 2;
      
      display_ = display;
      provider_ = display.getDataProvider();
      axisPanels_ = new ArrayList<AxisPanel>();

      File file = new File(display.getName());
      String shortName = file.getName();
      super.setTitle("Export Displayed Images: " + shortName);

      contentsPanel_ = new JPanel(new MigLayout("flowy"));

      JLabel help = new JLabel("<html><body>Export a series of images from your dataset. The images will be exactly as currently<br>drawn on your display, including histogram scaling, zoom, overlays, etc. Note that<br>this does not preserve the raw data, nor any metadata.</body></html>");
      contentsPanel_.add(help, "align center");

      if (getIsComposite()) {
         contentsPanel_.add(new JLabel("<html><body>The \"channel\" axis is unavailable as the display is in composite mode.</body></html>"),
               "align center");
      }

      final String LABEL_PREFIX = "Filename Prefix: ";
      final String LABEL_TITLE = "Image Title: ";

      contentsPanel_.add(new JLabel("Output: "),
            "split 4, flowx");
      outputFormatSelector_ = new JComboBox(OUTPUT_FORMATS);
      outputFormatSelector_.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            // Show/hide the JPEG quality controls.
            String selection = (String) outputFormatSelector_.getSelectedItem();
            if (selection.equals(FORMAT_JPEG)) {
               jpegPanel_.add(new JLabel("JPEG Quality: "));
               jpegPanel_.add(jpegQualitySpinner_);
            }
            else {
               jpegPanel_.removeAll();
            }
            prefixLabel_.setText(selection.equals(FORMAT_IMAGEJ) ?
                  LABEL_TITLE : LABEL_PREFIX);
            pack();
         }
      });
      contentsPanel_.add(outputFormatSelector_);
      prefixLabel_ = new JLabel(LABEL_PREFIX);
      contentsPanel_.add(prefixLabel_);

      prefixText_ = new JTextField(getDefaultPrefix(), 20);
      contentsPanel_.add(prefixText_, "grow 0");

      // We want this panel to take up minimal space when it is not needed.
      jpegPanel_ = new JPanel(new MigLayout("flowx, gap 0", "0[]0[]0",
               "0[]0[]0"));
      jpegQualitySpinner_ = new JSpinner();
      jpegQualitySpinner_.setModel(new SpinnerNumberModel(10, 3, 10, 1));

      contentsPanel_.add(jpegPanel_);

      if (getNonZeroAxes().isEmpty()) {
         // No iteration available.
         contentsPanel_.add(
               new JLabel("There is only one image available to export."),
               "align center");
      }
      else {
         contentsPanel_.add(createAxisPanel());
      }
      // Dropdown menu with all axes (except channel when in composite mode)
      // show channel note re: composite mode
      // show note about overlays
      // allow selecting range for each axis; "add axis" button which disables
      // when all axes are used
      // for single-axis datasets just auto-fill the one axis
      // Future req: add ability to export to ImageJ as RGB stack

      JButton cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            dispose();
         }
      });
      JButton exportButton = new JButton("Export");
      exportButton.addActionListener((ActionEvent event) -> {
         try {
            export();
         }
         catch (IOException e) {
            ReportingUtils.showError(e, "Cannot export image(s)", display_.getWindow());
         }
      });
      contentsPanel_.add(cancelButton, "split 2, flowx, align right");
      contentsPanel_.add(exportButton);

      super.getContentPane().add(contentsPanel_);
      outputFormatSelector_.setSelectedItem(getDefaultExportFormat());
      super.pack();
      super.setLocation(centerX - super.getWidth() / 2, 
              centerY - super.getHeight() / 2);
      super.setVisible(true);
   }

   private void export() throws IOException {
      ImageExporter.Builder builder = display_.imageExporterBuilder();

      // Set output format.
      String mode = (String) outputFormatSelector_.getSelectedItem();
      switch (mode) {
         case FORMAT_PNG:
            builder.output(ImageExporter.Destination.PNG_FILES);
            chooseDirectory(builder);
            break;
         case FORMAT_JPEG:
            builder.output(ImageExporter.Destination.JPEG_FILES);
            builder.jpegQuality((Integer) jpegQualitySpinner_.getValue() / 10.0f);
            chooseDirectory(builder);
            break;
         case FORMAT_IMAGEJ:
            builder.output(ImageExporter.Destination.IMAGEJ_RGB_STACK);
            break;
      }
      builder.filenamePrefix(prefixText_.getText());

      if (axisPanels_.size() > 0) {
         axisPanels_.get(0).configureExporter(builder);
      }
      // If there are no axes, we don't loop (obviously)

      ImageExporter exporter = builder.build();

      if (exporter.willPotentiallyOverwriteFiles()) {
         JOptionPane alert = new JOptionPane(
               "<html>The exported files will overwrite existing files or create files with similar filename patterns.<br />Are you sure you want to continue?</html>",
               JOptionPane.WARNING_MESSAGE, JOptionPane.DEFAULT_OPTION, null,
               new String[] {"Cancel", "Continue"}, "Cancel");
         alert.createDialog(this, "Overwrite Existing Files").show();
         if (!"Continue".equals(alert.getValue())) {
            exporter.close();
            return;
         }
      }

      setDefaultExportFormat(mode);
      setDefaultPrefix(prefixText_.getText());

      WaitDialog dialog = new WaitDialog("Exporting...");
      dialog.setAlwaysOnTop(true);
      dialog.showDialog();

      exporter.run(() -> exportFinished(exporter, dialog, null),
            (exception) -> exportFinished(exporter, dialog, exception));

      dispose();
   }

   private void exportFinished(ImageExporter exporter, WaitDialog dialog, Exception exception) {
      dialog.closeDialog();
      if (exception != null) {
         ReportingUtils.showError(exception, "Could not export images.", display_.getWindow());
      }
      try {
         exporter.close();
      }
      catch (IOException e) {
         if (exception == null) { // Don't show second error dialog
            ReportingUtils.showError(e, "Could not finish export correctly", display_.getWindow());
         }
         else {
            ReportingUtils.logError(e, "Could not finish export correctly");
         }
      }
   }

   private void chooseDirectory(ImageExporter.Builder builder) {
      // Get save path if relevant.
      File outputDir;
      // Prompt the user for a directory to save to.
      JFileChooser chooser = new JFileChooser();
      chooser.setDialogTitle("Please choose a directory to export to.");
      chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      chooser.setAcceptAllFileFilterUsed(false);
      if (provider_ instanceof Datastore) {
         Datastore store = (Datastore) provider_;
         if (store.getSavePath() != null) {
            // Default them to where their data was originally saved.
            File path = new File(store.getSavePath());
            chooser.setCurrentDirectory(path);
            chooser.setSelectedFile(path);
            // HACK: on OSX if we don't do this, the "Choose" button will be
            // disabled until the user interacts with the dialog.
            // This may be related to a bug in the OSX JRE; see
            // http://stackoverflow.com/questions/31148021/jfilechooser-cant-set-default-selection/31148287
            // and in particular Madhan's reply.
            chooser.updateUI();
         }
      }
      if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
         // User cancelled.
         return;
      }
      outputDir = chooser.getSelectedFile();
      // HACK: for unknown reasons, on OSX at least we can get a
      // repetition of the final directory if the user clicks the "Choose"
      // button when inside the directory they want to use, resulting in
      // e.g. /foo/bar/baz/baz when only /foo/bar/baz exists.
      if (!outputDir.exists()) {
         outputDir = new File(outputDir.getParent());
         if (!outputDir.exists()) {
            ReportingUtils.
                  showError("Unable to find directory at " + outputDir);
         }
      }
      builder.outputPath(outputDir);
   }

   /**
    * Create a row of controls for iterating over an axis. Pick an axis from
    * those not yet being used.
    * @return 
    */
   public AxisPanel createAxisPanel() {
      HashSet<String> axes = new HashSet<String>(getNonZeroAxes());
      for (AxisPanel panel : axisPanels_) {
         axes.remove(panel.getAxis());
      }
      if (axes.isEmpty()) {
         ReportingUtils.logError("Asked to create axis control when no more valid axes remain.");
         return null;
      }
      String axis = (new ArrayList<String>(axes)).get(0);

      AxisPanel panel = new AxisPanel(display_, this);
      panel.setAxis(axis);
      axisPanels_.add(panel);
      return panel;
   }

   /**
    * One of our panels is changing from the old axis to the new axis; if the
    * new axis is represented in any other panel, it must be swapped with the
    * old one.
    * @param oldAxis
    * @param newAxis
    */
   public void changeAxis(String oldAxis, String newAxis) {
      for (AxisPanel panel : axisPanels_) {
         if (panel.getAxis().equals(newAxis)) {
            panel.setAxis(oldAxis);
         }
      }
   }

   /**
    * Remove all AxisPanels after the specified panel. Note that the AxisPanel
    * passed into this method is responsible for removing the following panels
    * from the GUI.
    * @param last
    */
   public void deleteFollowing(AxisPanel last) {
      boolean shouldRemove = false;
      HashSet<AxisPanel> defuncts = new HashSet<AxisPanel>();
      for (AxisPanel panel : axisPanels_) {
         if (shouldRemove) {
            defuncts.add(panel);
         }
         if (panel == last) {
            shouldRemove = true;
         }
      }
      // Remove them from the listing.
      for (AxisPanel panel : defuncts) {
         axisPanels_.remove(panel);
      }
      pack();
   }

   /**
    * Returns true if the display mode is composite.
    */
   private boolean getIsComposite() {
      ImagePlus displayPlus = display_.getImagePlus();
      if (displayPlus instanceof CompositeImage) {
         return ((CompositeImage) displayPlus).getMode() == CompositeImage.COMPOSITE;
      }
      return false;
   }

   /**
    * Return the available axes (that exist in the datastore and have nonzero
    * length).
    * @return 
    */
   public ArrayList<String> getNonZeroAxes() {
      ArrayList<String> result = new ArrayList<String>();
      for (String axis : provider_.getAxes()) {
         // Channel axis is only available when in non-composite display modes.
         if (provider_.getMaxIndices().getIndex(axis) > 0 &&
               (!axis.equals(Coords.CHANNEL) || !getIsComposite())) {
            result.add(axis);
         }
      }
      return result;
   }

   /**
    * Return the number of axes that are not currently being used and that
    * have a nonzero length.
    * @return 
    */
   public int getNumSpareAxes() {
      return getNonZeroAxes().size() - axisPanels_.size();
   }

   /**
    * Get the default mode the user wants to use for exporting movies.
    */
   private static String getDefaultExportFormat() {
      return UserProfileStaticInterface.getInstance().
              getSettings(ExportMovieDlg.class).
              getString(DEFAULT_EXPORT_FORMAT, FORMAT_PNG);
   }

   /**
    * Set the default mode to use for exporting movies.
    */
   private static void setDefaultExportFormat(String format) {
      UserProfileStaticInterface.getInstance().getSettings(ExportMovieDlg.class).
              putString(DEFAULT_EXPORT_FORMAT, format);
   }

   /**
    * Get the default filename prefix.
    */
   private static String getDefaultPrefix() {
      return UserProfileStaticInterface.getInstance().
              getSettings(ExportMovieDlg.class).
              getString(DEFAULT_FILENAME_PREFIX, "Exported");
   }

   /**
    * Set a new default filename prefix.
    */
   private static void setDefaultPrefix(String prefix) {
      UserProfileStaticInterface.getInstance().
              getSettings(ExportMovieDlg.class).
              putString(DEFAULT_FILENAME_PREFIX, prefix);
   }
}

