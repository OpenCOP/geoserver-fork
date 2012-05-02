package org.geoserver.web.data.webeoc;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.store.StoreListChoiceRenderer;
import org.geoserver.web.data.store.StoreListModel;
import org.geoserver.web.data.store.WebEOCStoreListModel;

/**
 *
 * @author yancy
 */
public class CreateWebEOCLayerPage extends GeoServerSecuredPage {
  
  private final Form form;
  private final DropDownChoice stores;
  private final TextField<String> incident;
  private final TextField<String> board;
  private final TextField<String> view;
  private final TextField<String> layerTitle;
  
  public CreateWebEOCLayerPage() {
    
    // create the form
    form = new Form("form");
    add(form);
    
    // create the datastore picker, only include PostGIS stores
    stores = new DropDownChoice("storesDropDown", new Model(), 
             new WebEOCStoreListModel(), new StoreListChoiceRenderer());
    stores.setOutputMarkupId(true);
    stores.setRequired(true);
    form.add(stores);
    
    // create the title field
    form.add(incident = new TextField<String>("incident", new Model<String>()));
    // create the title field
    form.add(board = new TextField<String>("board", new Model<String>()));
    // create the title field
    form.add(view = new TextField<String>("view", new Model<String>()));
    // create the title field
    form.add(layerTitle = new TextField<String>("layerTitle", new Model<String>()));

    // create the save and cancel buttons
    form.add(new BookmarkablePageLink("cancel", GeoServerHomePage.class));
    SubmitLink saveLink = new SubmitLink("save", form) {
      @Override
      public void onSubmit() {
//        submit();
      }
    };
    form.add(saveLink);
    form.setDefaultButton(saveLink);
  }
}
