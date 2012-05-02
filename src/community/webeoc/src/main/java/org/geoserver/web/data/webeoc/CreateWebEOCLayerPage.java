package org.geoserver.web.data.webeoc;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerSecuredPage;

/**
 *
 * @author yancy
 */
public class CreateWebEOCLayerPage extends GeoServerSecuredPage {
  
  private final Form form;
  private final TextField<String> incident;
  private final TextField<String> board;
  private final TextField<String> view;
  
  public CreateWebEOCLayerPage() {
    
    // create the form
    form = new Form("form");
    add(form);
    
    // create the title field
    form.add(incident = new TextField<String>("incident", new Model<String>()));
    // create the title field
    form.add(board = new TextField<String>("board", new Model<String>()));
    // create the title field
    form.add(view = new TextField<String>("view", new Model<String>()));

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
