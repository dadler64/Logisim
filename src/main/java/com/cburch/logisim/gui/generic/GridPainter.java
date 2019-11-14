/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.generic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.MemoryImageSource;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;

public class GridPainter {

    public static final String ZOOM_PROPERTY = "zoom";
    private static final String SHOW_GRID_PROPERTY = "showgrid";

    private static final int GRID_DOT_COLOR = 0xFF777777;
    private static final int GRID_DOT_ZOOMED_COLOR = 0xFFCCCCCC;

    private static final Color GRID_ZOOMED_OUT_COLOR = new Color(210, 210, 210);
    private Component destination;
    private PropertyChangeSupport support;
    private Listener listener;
    private ZoomModel zoomModel;
    private boolean showGrid;
    private int gridSize;
    private double zoomFactor;
    private Image gridImage;
    private int gridImageWidth;

    public GridPainter(Component destination) {
        this.destination = destination;
        support = new PropertyChangeSupport(this);
        showGrid = true;
        gridSize = 10;
        zoomFactor = 1.0;
        updateGridImage(gridSize, zoomFactor);
    }

    public void addPropertyChangeListener(String property, PropertyChangeListener listener) {
        support.addPropertyChangeListener(property, listener);
    }

    public void removePropertyChangeListener(String property, PropertyChangeListener listener) {
        support.removePropertyChangeListener(property, listener);
    }

    public boolean getShowGrid() {
        return showGrid;
    }

    private void setShowGrid(boolean value) {
        if (showGrid != value) {
            showGrid = value;
            support.firePropertyChange(SHOW_GRID_PROPERTY, !value, value);
        }
    }

    public double getZoomFactor() {
        return zoomFactor;
    }

    private void setZoomFactor(double value) {
        double oldValue = zoomFactor;
        if (oldValue != value) {
            zoomFactor = value;
            updateGridImage(gridSize, value);
            support.firePropertyChange(ZOOM_PROPERTY, oldValue, value);
        }
    }

    public ZoomModel getZoomModel() {
        return zoomModel;
    }

    public void setZoomModel(ZoomModel model) {
        ZoomModel oldModel = this.zoomModel;
        if (model != oldModel) {
            if (listener == null) {
                listener = new Listener();
            }
            if (oldModel != null) {
                oldModel.removePropertyChangeListener(ZoomModel.ZOOM, listener);
                oldModel.removePropertyChangeListener(ZoomModel.SHOW_GRID, listener);
            }
            this.zoomModel = model;
            if (model != null) {
                model.addPropertyChangeListener(ZoomModel.ZOOM, listener);
                model.addPropertyChangeListener(ZoomModel.SHOW_GRID, listener);
            }
            assert model != null;
            setShowGrid(model.getShowGrid());
            setZoomFactor(model.getZoomFactor());
            destination.repaint();
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public void paintGrid(Graphics graphics) {
        Rectangle clip = graphics.getClipBounds();
        Component destination = this.destination;
        double zoomFactor = this.zoomFactor;
        int gridSize = this.gridSize;

        if (!showGrid) {
            return;
        }

        Image gridImage = this.gridImage;
        int imageWidth = this.gridImageWidth;
        if (gridImage == null) {
            paintGridOld(graphics, gridSize, zoomFactor, clip);
            return;
        }
        int x0 = (clip.x / imageWidth) * imageWidth; // round down to multiple of imageWidth
        int y0 = (clip.y / imageWidth) * imageWidth;
        for (int x = 0; x < clip.width + imageWidth; x += imageWidth) {
            for (int y = 0; y < clip.height + imageWidth; y += imageWidth) {
                graphics.drawImage(gridImage, x0 + x, y0 + y, destination);
            }
        }
    }

    private void paintGridOld(Graphics graphics, int size, double f, Rectangle clip) {
        graphics.setColor(Color.GRAY);
        if (f == 1.0) {
            int start_x = ((clip.x + 9) / size) * size;
            int start_y = ((clip.y + 9) / size) * size;
            for (int x = 0; x < clip.width; x += size) {
                for (int y = 0; y < clip.height; y += size) {
                    graphics.fillRect(start_x + x, start_y + y, 1, 1);
                }
            }
        } else {
            /* Kevin Walsh of Cornell suggested the code below instead. */
            int x0 = size * (int) Math.ceil(clip.x / f / size);
            int x1 = x0 + (int) (clip.width / f);
            int y0 = size * (int) Math.ceil(clip.y / f / size);
            int y1 = y0 + (int) (clip.height / f);
            if (f <= 0.5) {
                graphics.setColor(GRID_ZOOMED_OUT_COLOR);
            }
            for (double x = x0; x < x1; x += size) {
                for (double y = y0; y < y1; y += size) {
                    int sx = (int) Math.round(f * x);
                    int sy = (int) Math.round(f * y);
                    graphics.fillRect(sx, sy, 1, 1);
                }
            }
            if (f <= 0.5) { // make every 5th pixel darker
                int sizeSpacedOut = 5 * size;
                graphics.setColor(Color.GRAY);
                x0 = sizeSpacedOut * (int) Math.ceil(clip.x / f / sizeSpacedOut);
                y0 = sizeSpacedOut * (int) Math.ceil(clip.y / f / sizeSpacedOut);
                for (double x = x0; x < x1; x += sizeSpacedOut) {
                    for (double y = y0; y < y1; y += sizeSpacedOut) {
                        int sx = (int) Math.round(f * x);
                        int sy = (int) Math.round(f * y);
                        graphics.fillRect(sx, sy, 1, 1);
                    }
                }
            }

			/* Original code by Carl Burch
			int x0 = 10 * (int) Math.ceil(clip.x / f / 10);
			int x1 = x0 + (int)(clip.width / f);
			int y0 = 10 * (int) Math.ceil(clip.y / f / 10);
			int y1 = y0 + (int) (clip.height / f);
			int s = f > 0.5 ? 1 : f > 0.25 ? 2 : 3;
			int i0 = s - ((x0 + 10*s - 1) % (s * 10)) / 10 - 1;
			int j0 = s - ((y1 + 10*s - 1) % (s * 10)) / 10 - 1;
			for (int i = 0; i < s; i++) {
				for (int x = x0+i*10; x < x1; x += s*10) {
					for (int j = 0; j < s; j++) {
						g.setColor(i == i0 && j == j0 ? Color.gray : GRID_ZOOMED_OUT_COLOR);
						for (int y = y0+j*10; y < y1; y += s*10) {
							int sx = (int) Math.round(f * x);
							int sy = (int) Math.round(f * y);
							g.fillRect(sx, sy, 1, 1);
						}
					}
				}
			}
			*/
        }
    }

    //
    // creating the grid image
    //
    @SuppressWarnings("SuspiciousNameCombination")
    private void updateGridImage(int size, double f) {
        double ww = f * size * 5;
        while (2 * ww < 150) {
            ww *= 2;
        }
        int width = (int) Math.round(ww);
        int[] pixels = new int[width * width];
        Arrays.fill(pixels, 0xFFFFFF);

        if (f == 1.0) {
            int lineStep = size * width;
            for (int j = 0; j < pixels.length; j += lineStep) {
                for (int i = 0; i < width; i += size) {
                    pixels[i + j] = GRID_DOT_COLOR;
                }
            }
        } else {
            int offset0 = 0;
            int offset1 = 1;
            if (f >= 2.0) { // we'll draw several pixels for each grid point
                int num = (int) (f + 0.001);
                offset0 = -(num / 2);
                offset1 = offset0 + num;
            }

            int dotColor = f <= 0.5 ? GRID_DOT_ZOOMED_COLOR : GRID_DOT_COLOR;
            for (int j = 0; true; j += size) {
                int y = (int) Math.round(f * j);
                if (y + offset0 >= width) {
                    break;
                }

                for (int yo = y + offset0; yo < y + offset1; yo++) {
                    if (yo >= 0 && yo < width) {
                        int base = yo * width;
                        for (int i = 0; true; i += size) {
                            int x = (int) Math.round(f * i);
                            if (x + offset0 >= width) {
                                break;
                            }
                            for (int xo = x + offset0; xo < x + offset1; xo++) {
                                if (xo >= 0 && xo < width) {
                                    pixels[base + xo] = dotColor;
                                }
                            }
                        }
                    }
                }
            }
            if (f <= 0.5) { // repaint over every 5th pixel so it is darker
                int sizeSpacedOut = size * 5;
                for (int j = 0; true; j += sizeSpacedOut) {
                    int y = (int) Math.round(f * j);
                    if (y >= width) {
                        break;
                    }
                    y *= width;

                    for (int i = 0; true; i += sizeSpacedOut) {
                        int x = (int) Math.round(f * i);
                        if (x >= width) {
                            break;
                        }
                        pixels[y + x] = GRID_DOT_COLOR;
                    }
                }
            }
        }
        gridImage = destination.createImage(new MemoryImageSource(width, width, pixels, 0, width));
        gridImageWidth = width;
    }

    private class Listener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent event) {
            String property = event.getPropertyName();
            Object value = event.getNewValue();
            if (property.equals(ZoomModel.ZOOM)) {
                setZoomFactor((Double) value);
                destination.repaint();
            } else if (property.equals(ZoomModel.SHOW_GRID)) {
                setShowGrid((Boolean) value);
                destination.repaint();
            }
        }
    }
}
