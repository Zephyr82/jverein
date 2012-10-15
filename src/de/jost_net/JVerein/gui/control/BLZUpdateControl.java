/**********************************************************************
 * $Source$
 * $Revision$
 * $Date$
 * $Author$
 *
 * Copyright (c) by Heiner Jostkleigrewe
 * This program is free software: you can redistribute it and/or modify it under the terms of the 
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without 
 *  even the implied warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See 
 *  the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If not, 
 * see <http://www.gnu.org/licenses/>.
 * 
 * heiner@jverein.de
 * www.jverein.de
 **********************************************************************/
package de.jost_net.JVerein.gui.control;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;

import de.jost_net.JVerein.Einstellungen;
import de.jost_net.JVerein.JVereinPlugin;
import de.jost_net.JVerein.io.BLZDatei;
import de.jost_net.JVerein.io.BLZSatz;
import de.jost_net.JVerein.io.BLZUpdate;
import de.jost_net.JVerein.rmi.Mitglied;
import de.willuhn.datasource.GenericIterator;
import de.willuhn.datasource.GenericObject;
import de.willuhn.datasource.pseudo.PseudoIterator;
import de.willuhn.datasource.rmi.DBIterator;
import de.willuhn.jameica.gui.AbstractControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.Part;
import de.willuhn.jameica.gui.input.TextInput;
import de.willuhn.jameica.gui.parts.Button;
import de.willuhn.jameica.gui.parts.TablePart;
import de.willuhn.jameica.system.Settings;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

public class BLZUpdateControl extends AbstractControl
{
  private Settings settings;

  private TextInput blzdatei;

  private TablePart blzupdateList;

  public BLZUpdateControl(AbstractView view)
  {
    super(view);
    settings = new de.willuhn.jameica.system.Settings(this.getClass());
    settings.setStoreWhenRead(true);
  }

  public final TextInput getBlzdatei() throws RemoteException
  {
    if (blzdatei != null)
    {
      return blzdatei;
    }
    blzdatei = new TextInput("", 100);
    blzdatei.setName(JVereinPlugin.getI18n().tr("BLZ-Datei"));
    return blzdatei;
  }

  public Button getDateiauswahlButton()
  {
    Button b = new Button(JVereinPlugin.getI18n().tr(
        "Bundesbankdatei ausw�hlen"), new Action()
    {

      @Override
      public void handleAction(Object context) throws ApplicationException
      {
        settings = new Settings(this.getClass());
        settings.setStoreWhenRead(true);

        FileDialog fd = new FileDialog(GUI.getShell(), SWT.OPEN);
        String path = settings.getString("lastdir",
            System.getProperty("user.home"));
        if (path != null && path.length() > 0)
        {
          fd.setFilterPath(path);
        }

        fd.setText(JVereinPlugin.getI18n().tr("BLZ-Datei der Bundesdruckerei."));
        fd.setFilterExtensions(new String[] { "*.ZIP" });
        fd.setFilterNames(new String[] { JVereinPlugin.getI18n().tr(
            "ZIP-Datei der Bundesbank") });

        String f = fd.open();
        try
        {
          getBlzdatei().setValue(f);
          File blzdatei = new File(f);
          settings.setAttribute("lastdir", blzdatei.getParent());
          ermittleUpdates(f);
          if (blzupdateList.getItems().isEmpty())
          {
            GUI.getStatusBar().setSuccessText(
                JVereinPlugin.getI18n().tr(
                    "Alle Bankleitzahlen sind auf dem aktuellen Stand"));
          }
        }
        catch (RemoteException e)
        {
          Logger.error(JVereinPlugin.getI18n().tr("Fehler"), e);
        }
        catch (IOException e)
        {
          Logger.error(JVereinPlugin.getI18n().tr("Fehler"), e);
        }
      }
    }, null, true, "adler.png"); // "true" defines this button as the default
    // button
    return b;
  }

  public Button getSpeichernButton()
  {
    Button b = new Button(JVereinPlugin.getI18n().tr("speichern"), new Action()
    {
      @Override
      public void handleAction(Object context) throws ApplicationException
      {
        try
        {
          List objects = blzupdateList.getItems();
          for (Object o : objects)
          {
            BLZUpdate blzu = (BLZUpdate) o;
            Mitglied m = (Mitglied) blzu.getAttribute("mitglied");
            m.setBlz((String) blzu.getAttribute("newblz"));
            m.store();
          }
          GUI.getStatusBar().setSuccessText(
              JVereinPlugin.getI18n().tr("Bankleitzahlen aktualisiert"));
        }
        catch (RemoteException e)
        {
          e.printStackTrace();
        }
      }
    }, null, false, "document-save.png");
    return b;
  }

  public Part getList() throws RemoteException
  {
    blzupdateList = new TablePart(getIterator(), null);
    blzupdateList.addColumn(JVereinPlugin.getI18n().tr("Name, Vorname"),
        "namevorname");
    blzupdateList.addColumn(JVereinPlugin.getI18n().tr("alte BLZ"), "oldblz");
    blzupdateList.addColumn(("neue BLZ"), "newblz");
    blzupdateList.setRememberColWidths(true);
    blzupdateList.setRememberOrder(true);
    blzupdateList.setSummary(true);
    return blzupdateList;
  }

  private GenericIterator getIterator() throws RemoteException
  {
    ArrayList<BLZUpdate> zeile = new ArrayList<BLZUpdate>();

    GenericIterator gi = PseudoIterator.fromArray(zeile
        .toArray(new GenericObject[zeile.size()]));
    return gi;
  }

  private void ermittleUpdates(String file) throws IOException
  {
    BLZDatei blz = new BLZDatei(file);
    BLZSatz satz = blz.getNext();
    while (satz.hasNext())
    {
      if (satz.getZahlungsdienstleister().equals("1")
          && !satz.getNachfolgeblz().equals("00000000"))
      {
        DBIterator it = Einstellungen.getDBService().createList(Mitglied.class);
        it.addFilter("blz = ?", satz.getBlz());
        while (it.hasNext())
        {
          Mitglied m = (Mitglied) it.next();
          blzupdateList.addItem(new BLZUpdate(m, satz.getNachfolgeblz()));
        }
      }
      satz = blz.getNext();
    }
  }
}
