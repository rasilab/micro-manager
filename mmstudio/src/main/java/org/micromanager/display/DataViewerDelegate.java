package org.micromanager.display;

/**
 * An interface that allows attaching custom behavior to a {@code DataViewer},
 * independent of the actual concrete type of the {@code DataViewer}.
 * 
 * @author Nico Stuurman, Mark Tsuchida
 */
public interface DataViewerDelegate {
   /**
    * Determine (by potentially prompting the user to save) whether the viewer
    * should close.
    * <p>
    * It is possible to defer the decision to another
    * {@code DataViewerDelegate} by setting it as the viewer's delegate, and
    * returning true.
    *
    * @param viewer the viewer that is trying to close
    * @return true if the viewer should continue the closing process; false if
    * the closing should be canceled
    */
   boolean dataViewerShouldClose(DataViewer viewer);
}