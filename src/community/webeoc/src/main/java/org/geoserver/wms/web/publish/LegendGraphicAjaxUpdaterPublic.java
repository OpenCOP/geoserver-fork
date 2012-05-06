package org.geoserver.wms.web.publish;

import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.IModel;

/**
 * This class is just a public extension of LegendGraphicAjaxUpdater.
 *
 * @author yancy
 */
public class LegendGraphicAjaxUpdaterPublic extends LegendGraphicAjaxUpdater {

  public LegendGraphicAjaxUpdaterPublic(final String wmsURL, final Image image,
          final IModel styleInfoModel) {
    super(wmsURL, image, styleInfoModel);
  }
}
