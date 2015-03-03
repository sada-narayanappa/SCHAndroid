package org.geospaces.schas.fragment_resources;

/**
 * Created by Student on 3/1/2015.
 */
public class InfoListItem {
    private String itemTitle;

    public String getItemTitle(){
        return itemTitle;
    }

    public void setItemTitle(String itemTitle){
        this.itemTitle = itemTitle;
    }

    public InfoListItem(String title){
        this.itemTitle = title;
    }
}
