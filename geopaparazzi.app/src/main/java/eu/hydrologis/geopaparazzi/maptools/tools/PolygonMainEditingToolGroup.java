/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.hydrologis.geopaparazzi.maptools.tools;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.PorterDuff.Mode;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.vividsolutions.jts.geom.Geometry;

import org.mapsforge.android.maps.MapView;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.features.EditManager;
import eu.geopaparazzi.library.features.Feature;
import eu.geopaparazzi.library.features.ILayer;
import eu.geopaparazzi.library.features.Tool;
import eu.geopaparazzi.library.features.ToolGroup;
import eu.geopaparazzi.library.util.Compat;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.spatialite.database.spatial.SpatialiteSourcesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.daos.DaoSpatialite;
import eu.geopaparazzi.spatialite.database.spatial.core.enums.GeometryType;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.maptools.FeatureUtilities;
import eu.hydrologis.geopaparazzi.mapview.MapsSupportService;

/**
 * The main polygon layer editing tool group, which just shows the tool palette.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PolygonMainEditingToolGroup implements ToolGroup, OnClickListener, OnTouchListener {

    private ImageButton selectAllButton;
    private MapView mapView;

    private ImageButton selectEditableButton;
    private int selectionColor;
    private ImageButton createFeatureButton;
    private ImageButton cutButton;
    private ImageButton extendButton;
    private ImageButton commitButton;

    private ImageButton undoButton;
    private Feature cutExtendProcessedFeature;
    private Feature cutExtendFeatureToRemove;

    /**
     * Constructor.
     *
     * @param mapView the map view.
     */
    public PolygonMainEditingToolGroup(MapView mapView) {
        this.mapView = mapView;

        LinearLayout parent = EditManager.INSTANCE.getToolsLayout();
        selectionColor = Compat.getColor(parent.getContext(), R.color.main_selection);
    }

    public void activate() {
        if (mapView != null)
            mapView.setClickable(true);
    }

    public void initUI() {

        LinearLayout parent = EditManager.INSTANCE.getToolsLayout();
        Context context = parent.getContext();
        ILayer editLayer = EditManager.INSTANCE.getEditLayer();
        int padding = 2;

        if (editLayer != null) {
            cutButton = new ImageButton(context);
            cutButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            cutButton.setBackground(Compat.getDrawable(context, R.drawable.editing_cut));
            cutButton.setPadding(0, padding, 0, padding);
            cutButton.setOnClickListener(this);
            cutButton.setOnTouchListener(this);
            parent.addView(cutButton);

            extendButton = new ImageButton(context);
            extendButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            extendButton.setBackground(Compat.getDrawable(context, R.drawable.editing_extend));
            extendButton.setPadding(0, padding, 0, padding);
            extendButton.setOnClickListener(this);
            extendButton.setOnTouchListener(this);
            parent.addView(extendButton);

            createFeatureButton = new ImageButton(context);
            createFeatureButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
            createFeatureButton.setBackground(Compat.getDrawable(context, R.drawable.editing_create_polygon));
            createFeatureButton.setPadding(0, padding, 0, padding);
            createFeatureButton.setOnClickListener(this);
            createFeatureButton.setOnTouchListener(this);
            parent.addView(createFeatureButton);

            selectEditableButton = new ImageButton(context);
            selectEditableButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
            selectEditableButton.setBackground(Compat.getDrawable(context, R.drawable.editing_select_editable));
            selectEditableButton.setPadding(0, padding, 0, padding);
            selectEditableButton.setOnClickListener(this);
            selectEditableButton.setOnTouchListener(this);
            parent.addView(selectEditableButton);
        }

        selectAllButton = new ImageButton(context);
        selectAllButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        selectAllButton.setBackground(Compat.getDrawable(context, R.drawable.editing_select_all));
        selectAllButton.setPadding(0, padding, 0, padding);
        selectAllButton.setOnClickListener(this);
        selectAllButton.setOnTouchListener(this);
        parent.addView(selectAllButton);

        if (editLayer != null) {
            undoButton = new ImageButton(context);
            undoButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            undoButton.setBackground(Compat.getDrawable(context, R.drawable.editing_undo));
            undoButton.setPadding(0, padding, 0, padding);
            undoButton.setOnTouchListener(this);
            undoButton.setOnClickListener(this);
            parent.addView(undoButton);
            undoButton.setVisibility(View.GONE);

            commitButton = new ImageButton(context);
            commitButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            commitButton.setBackground(Compat.getDrawable(context, R.drawable.editing_commit));
            commitButton.setPadding(0, padding, 0, padding);
            commitButton.setOnTouchListener(this);
            commitButton.setOnClickListener(this);
            parent.addView(commitButton);
            commitButton.setVisibility(View.GONE);
        }
    }

    public void disable() {
        EditManager.INSTANCE.setActiveTool(null);
        LinearLayout parent = EditManager.INSTANCE.getToolsLayout();
        if (parent != null)
            parent.removeAllViews();
    }

    public void onClick(View v) {
        if (v == selectAllButton) {
            Tool currentTool = EditManager.INSTANCE.getActiveTool();
            if (currentTool != null && currentTool instanceof InfoTool) {
                // if the same tool is re-selected, it is disabled
                EditManager.INSTANCE.setActiveTool(null);
            } else {
                // check maps enablement
                try {
                    final Collection<SpatialVectorTable> spatialTables = SpatialiteSourcesManager.INSTANCE.getSpatialiteMaps2TablesMap().values();
                    boolean atLeastOneEnabled = false;
                    for (SpatialVectorTable spatialVectorTable : spatialTables) {
                        if (spatialVectorTable.getStyle().enabled == 1) {
                            atLeastOneEnabled = true;
                            break;
                        }
                    }
                    if (!atLeastOneEnabled) {
                        LinearLayout parent = EditManager.INSTANCE.getToolsLayout();
                        if (parent != null) {
                            Context context = parent.getContext();
                            GPDialogs.warningDialog(context, context.getString(R.string.no_queriable_layer_is_visible), null);
                        }
                        return;
                    }
                } catch (Exception e) {
                    GPLog.error(this, null, e);
                }

                Tool activeTool = new InfoTool(this, mapView);
                EditManager.INSTANCE.setActiveTool(activeTool);
            }
        } else if (v == selectEditableButton) {
            Tool currentTool = EditManager.INSTANCE.getActiveTool();
            if (currentTool != null && currentTool instanceof SelectionTool) {
                // if the same tool is re-selected, it is disabled
                EditManager.INSTANCE.setActiveTool(null);
            } else {
                Tool activeTool = new SelectionTool(mapView);
                EditManager.INSTANCE.setActiveTool(activeTool);
            }
        } else if (v == createFeatureButton) {
            ToolGroup createFeatureToolGroup = new PolygonCreateFeatureToolGroup(mapView);
            EditManager.INSTANCE.setActiveToolGroup(createFeatureToolGroup);
        } else if (v == cutButton) {
            Tool currentTool = EditManager.INSTANCE.getActiveTool();
            if (currentTool != null && currentTool instanceof PolygonCutExtendTool) {
                // if the same tool is re-selected, it is disabled
                EditManager.INSTANCE.setActiveTool(null);
            } else {
                Tool activeTool = new PolygonCutExtendTool(mapView, true);
                EditManager.INSTANCE.setActiveTool(activeTool);
            }
        } else if (v == extendButton) {
            Tool currentTool = EditManager.INSTANCE.getActiveTool();
            if (currentTool != null && currentTool instanceof PolygonCutExtendTool) {
                // if the same tool is re-selected, it is disabled
                EditManager.INSTANCE.setActiveTool(null);
            } else {
                Tool activeTool = new PolygonCutExtendTool(mapView, false);
                EditManager.INSTANCE.setActiveTool(activeTool);
            }
        } else if (v == commitButton) {
            if (cutExtendProcessedFeature != null && cutExtendFeatureToRemove != null) {
                // substitute the feature's geometry in the db
                try {

                    SpatialVectorTable spatialVectorTable = SpatialiteSourcesManager.INSTANCE.getTableFromFeature(cutExtendProcessedFeature);
                    int tableGeomTypeCode = spatialVectorTable.getGeomType();
                    GeometryType tableGeometryType = GeometryType.forValue(tableGeomTypeCode);

                    Geometry newGeom = FeatureUtilities.WKBREADER.read(cutExtendProcessedFeature.getDefaultGeometry());

                    Context context = v.getContext();
                    if (tableGeometryType.isGeometryCompatible(newGeom)) {
                        DaoSpatialite.updateFeatureGeometry(
                                cutExtendProcessedFeature.getId(),
                                newGeom, LibraryConstants.SRID_WGS84_4326, spatialVectorTable);

                        DaoSpatialite.deleteFeatures(Collections.singletonList(cutExtendFeatureToRemove));
                        // reset mapview
                        Intent intent = new Intent(context, MapsSupportService.class);
                        intent.putExtra(MapsSupportService.REREAD_MAP_REQUEST, true);
                        context.startService(intent);
                    } else {
                        GPDialogs.warningDialog(context, context.getString(R.string.geom_incompatible_with_layer), null);
                        return;
                    }

                    EditManager.INSTANCE.setActiveTool(null);
                    commitButton.setVisibility(View.GONE);
                    undoButton.setVisibility(View.GONE);
                    EditManager.INSTANCE.invalidateEditingView();
                } catch (Exception e) {
                    GPLog.error(this, null, e); //$NON-NLS-1$
                }
            }
        } else if (v == undoButton) {
            if (cutExtendProcessedFeature != null) {
                EditManager.INSTANCE.setActiveTool(null);
                commitButton.setVisibility(View.GONE);
                undoButton.setVisibility(View.GONE);
                EditManager.INSTANCE.invalidateEditingView();
            }
        }

        handleToolIcons(v);
    }

    @SuppressWarnings("deprecation")
    private void handleToolIcons(View activeToolButton) {
        Context context = activeToolButton.getContext();
        Tool currentTool = EditManager.INSTANCE.getActiveTool();
        if (selectEditableButton != null) {
            if (currentTool != null && activeToolButton == selectEditableButton) {
                selectEditableButton.setBackground(Compat.getDrawable(context,
                        R.drawable.editing_select_editable_active));
            } else {
                selectEditableButton.setBackground(Compat.getDrawable(context,
                        R.drawable.editing_select_editable));
            }
        }
        if (selectAllButton != null)
            if (currentTool != null && activeToolButton == selectAllButton) {
                selectAllButton
                        .setBackground(Compat.getDrawable(context, R.drawable.editing_select_all_active));
            } else {
                selectAllButton.setBackground(Compat.getDrawable(context, R.drawable.editing_select_all));
            }
        if (cutButton != null)
            if (currentTool != null && activeToolButton == cutButton) {
                cutButton.setBackground(Compat.getDrawable(context, R.drawable.editing_cut_active));
            } else {
                cutButton.setBackground(Compat.getDrawable(context, R.drawable.editing_cut));
            }
        if (extendButton != null)
            if (currentTool != null && activeToolButton == extendButton) {
                extendButton.setBackground(Compat.getDrawable(context, R.drawable.editing_extend_active));
            } else {
                extendButton.setBackground(Compat.getDrawable(context, R.drawable.editing_extend));
            }

    }

    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                v.getBackground().setColorFilter(selectionColor, Mode.SRC_ATOP);
                v.invalidate();
                break;
            }
            case MotionEvent.ACTION_UP: {
                v.getBackground().clearColorFilter();
                v.invalidate();
                break;
            }
        }
        return false;
    }

    public void onToolFinished(Tool tool) {
        if (tool instanceof PolygonCutExtendTool) {
            PolygonCutExtendTool cutExtendTool = (PolygonCutExtendTool) tool;
            Feature[] processedFeatures = cutExtendTool.getProcessedFeatures();
            cutExtendProcessedFeature = processedFeatures[0];
            cutExtendFeatureToRemove = processedFeatures[1];

            commitButton.setVisibility(View.VISIBLE);
            undoButton.setVisibility(View.VISIBLE);
        }
        // if (activeTool == null) {
        // return;
        // }
        // if (tool == activeTool) {
        // sliderDrawView.disableTool();
        // activeTool.disable();
        // activeTool = null;
        // }
    }

    public void onToolDraw(Canvas canvas) {
        // nothing to draw
    }

    public boolean onToolTouchEvent(MotionEvent event) {
        return false;
    }

    public void onGpsUpdate(double lon, double lat) {
        // ignore
    }
}
