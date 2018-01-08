/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.micromanager.display.internal;

import org.micromanager.display.internal.event.DataViewerDidBecomeInactiveEvent;
import org.micromanager.display.internal.event.DataViewerWillCloseEvent;
import org.micromanager.display.internal.event.DataViewerDidBecomeActiveEvent;
import org.micromanager.display.internal.event.DataViewerDidBecomeVisibleEvent;
import org.micromanager.display.internal.event.DataViewerAddedEvent;
import org.micromanager.display.internal.event.DataViewerDidBecomeInvisibleEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.micromanager.EventPublisher;
import org.micromanager.display.DataViewer;
import org.micromanager.internal.utils.EventBusExceptionLogger;
import org.micromanager.internal.utils.MustCallOnEDT;

/**
 * The collection of data viewers.
 *
 * Manages all data viewers in the application, and keeps track of the
 * currently active viewer. Also publishes all viewer events.
 * <p>
 * The active viewer is the viewer currently selected by the user: for viewers
 * contained in their own window and it is the front window, that viewer is
 * active. Viewers that are subcomponents of other windows are active when
 * their containing window is in front (and the viewer is explicitly selected,
 * in the case where the window contains more than one viewer).
 * <p>
 * It is possible to have no active viewer (e.g. if the active window does not
 * contain any viewer).
 * <p>
 * This class handles generic {@code DataViewer}s and does not perform tasks
 * that are specific to {@code DisplayWindow}s.
 *
 * @author Mark A. Tsuchida
 */
public class DataViewerCollection implements EventPublisher {
   // Viewers known to this manager
   // Access: only on EDT
   private final Set<DataViewer> viewers_ = new HashSet<DataViewer>();

   // The currently active viewer, or null if none is active.
   // Access: only onEDT
   private DataViewer activeViewer_;

   private final EventBus eventBus_ = new EventBus(EventBusExceptionLogger.getInstance());

   public static DataViewerCollection create() {
      return new DataViewerCollection();
   }

   private DataViewerCollection() {
   }

   @MustCallOnEDT
   public boolean hasDataViewer(DataViewer viewer) {
      return viewers_.contains(viewer);
   }

   // Caveat: new viewer must be added before showing (and thus activating)
   @MustCallOnEDT
   public void addDataViewer(DataViewer viewer) {
      if (viewers_.contains(viewer)) {
         throw new IllegalArgumentException("DataViewer is already in collection");
      }
      viewers_.add(viewer);
      viewer.registerForEvents(this);
      eventBus_.post(DataViewerAddedEvent.create(viewer));
   }

   @MustCallOnEDT
   public void removeDataViewer(DataViewer viewer) {
      if (!viewers_.contains(viewer)) {
         throw new IllegalArgumentException("DataViewer is not in collection");
      }
      viewer.unregisterForEvents(this);
      viewers_.remove(viewer);
      if (viewer == getActiveDataViewer()) {
         eventBus_.post(DataViewerDidBecomeInactiveEvent.create(viewer));
         activeViewer_ = null;
      }
   }

   @MustCallOnEDT
   public List<DataViewer> getAllDataViewers() {
      return new ArrayList<DataViewer>(viewers_);
   }

   @MustCallOnEDT
   public DataViewer getActiveDataViewer() {
      return activeViewer_;
   }

   @Subscribe
   public void onEvent(DataViewerDidBecomeVisibleEvent e) {
      eventBus_.post(e);
   }

   @Subscribe
   public void onEvent(DataViewerDidBecomeInvisibleEvent e) {
      eventBus_.post(e);
   }

   @Subscribe
   public void onEvent(DataViewerDidBecomeActiveEvent e) {
      // Since data viewers are not able to know when they become inactive
      // (defined as another viewer becoming active, or the viewer closing),
      // we generate that event here.
      DataViewer previous = getActiveDataViewer();
      if (previous != null) {
         if (previous == e.getDataViewer()) { // No change
            return;
         }
         eventBus_.post(DataViewerDidBecomeInactiveEvent.create(previous));
      }

      activeViewer_ = e.getDataViewer();

      eventBus_.post(e);
   }

   @Subscribe
   public void onEvent(DataViewerWillCloseEvent e) {
      eventBus_.post(e);
      removeDataViewer(e.getDataViewer());
   }

   @Override
   public void registerForEvents(Object recipient) {
      eventBus_.register(recipient);
   }

   @Override
   public void unregisterForEvents(Object recipient) {
      eventBus_.unregister(recipient);
   }
}