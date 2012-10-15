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
package de.jost_net.JVerein.gui.action;

import de.jost_net.JVerein.JVereinPlugin;
import de.jost_net.JVerein.gui.control.FamilienbeitragNode;
import de.jost_net.JVerein.gui.dialogs.FamilienmitgliedEntfernenDialog;
import de.willuhn.jameica.gui.Action;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

public class FamilienmitgliedEntfernenAction implements Action
{
  @Override
  public void handleAction(Object context) throws ApplicationException
  {
    if (context == null || !(context instanceof FamilienbeitragNode))
    {
      throw new ApplicationException(JVereinPlugin.getI18n().tr("kein Familienmitglied ausgewählt"));
    }
    FamilienbeitragNode fbn = (FamilienbeitragNode) context;
    FamilienmitgliedEntfernenDialog fed = new FamilienmitgliedEntfernenDialog(
        fbn);
    try
    {
      fed.open();
    }

    catch (Exception e)
    {
      Logger.error("Fehler", e);
      return;
    }
  }
}
