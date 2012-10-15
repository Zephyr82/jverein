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
package de.jost_net.JVerein.server;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import de.jost_net.JVerein.Einstellungen;
import de.jost_net.JVerein.JVereinPlugin;
import de.jost_net.JVerein.Variable.BuchungVar;
import de.jost_net.JVerein.rmi.Abrechnungslauf;
import de.jost_net.JVerein.rmi.Buchung;
import de.jost_net.JVerein.rmi.Buchungsart;
import de.jost_net.JVerein.rmi.Jahresabschluss;
import de.jost_net.JVerein.rmi.Konto;
import de.jost_net.JVerein.rmi.Mitgliedskonto;
import de.jost_net.JVerein.rmi.Projekt;
import de.jost_net.JVerein.rmi.Spendenbescheinigung;
import de.jost_net.JVerein.util.JVDateFormatTTMMJJJJ;
import de.jost_net.JVerein.util.StringTool;
import de.willuhn.datasource.db.AbstractDBObject;
import de.willuhn.datasource.rmi.DBIterator;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

public class BuchungImpl extends AbstractDBObject implements Buchung
{

  private static final long serialVersionUID = 1L;

  public BuchungImpl() throws RemoteException
  {
    super();
  }

  @Override
  protected String getTableName()
  {
    return "buchung";
  }

  @Override
  public String getPrimaryAttribute()
  {
    return "id";
  }

  @Override
  protected void deleteCheck() throws ApplicationException
  {
    insertCheck();
  }

  @Override
  protected void insertCheck() throws ApplicationException
  {
    try
    {
      plausi();
    }
    catch (RemoteException e)
    {
      Logger.error(JVereinPlugin.getI18n().tr("Fehler"), e);
      throw new ApplicationException(JVereinPlugin.getI18n().tr(
          "Buchung kann nicht gespeichert werden. Siehe system log"));
    }
  }

  private void plausi() throws RemoteException, ApplicationException
  {
    if (getKonto() == null)
    {
      throw new ApplicationException(JVereinPlugin.getI18n().tr(
          "Bitte Konto eingeben"));
    }
    if (getDatum() == null)
    {
      throw new ApplicationException(JVereinPlugin.getI18n().tr(
          "Bitte Datum eingeben"));
    }
    Calendar cal1 = Calendar.getInstance();
    cal1.setTime(getDatum());
    Calendar cal2 = Calendar.getInstance();
    if (cal1.after(cal2))
    {
      throw new ApplicationException(JVereinPlugin.getI18n().tr(
          "Buchungsdatum liegt in der Zukunft"));
    }
    cal2.add(Calendar.YEAR, -10);
    if (cal1.before(cal2))
    {
      throw new ApplicationException(JVereinPlugin.getI18n().tr(
          "Buchung liegt mehr als 10 Jahre zur�ck"));
    }

    Jahresabschluss ja = getJahresabschluss();
    if (ja != null)
    {
      throw new ApplicationException(
          JVereinPlugin
              .getI18n()
              .tr("Buchung kann nicht gespeichert werden. Zeitraum ist bereits abgeschlossen!"));
    }
  }

  @Override
  protected void updateCheck() throws ApplicationException
  {
    insertCheck();
  }

  @Override
  protected Class<?> getForeignObject(String field)
  {
    if ("mitgliedskonto".equals(field))
    {
      return Mitgliedskonto.class;
    }
    else if ("abrechnungslauf".equals(field))
    {
      return Abrechnungslauf.class;
    }
    else if ("spendenbescheinigung".equals(field))
    {
      return Spendenbescheinigung.class;
    }
    else if ("projekt".equals(field))
    {
      return Projekt.class;
    }
    return null;
  }

  @Override
  public Integer getUmsatzid() throws RemoteException
  {

    return (Integer) getAttribute("umsatzid");
  }

  @Override
  public void setUmsatzid(Integer umsatzid) throws RemoteException
  {
    setAttribute("umsatzid", umsatzid);
  }

  @Override
  public Konto getKonto() throws RemoteException
  {
    Integer i = (Integer) super.getAttribute("konto");
    if (i == null)
      return null; // Kein Konto zugeordnet

    Cache cache = Cache.get(Konto.class, true);
    return (Konto) cache.get(i);
  }

  @Override
  public void setKonto(Konto konto) throws RemoteException
  {
    if (konto == null)
    {
      throw new RemoteException(JVereinPlugin.getI18n().tr("Konto fehlt!"));
    }
    setAttribute("konto", new Integer(konto.getID()));
  }

  @Override
  public Integer getAuszugsnummer() throws RemoteException
  {
    return (Integer) getAttribute("auszugsnummer");
  }

  @Override
  public void setAuszugsnummer(Integer auszugsnummer) throws RemoteException
  {
    setAttribute("auszugsnummer", auszugsnummer);
  }

  @Override
  public Integer getBlattnummer() throws RemoteException
  {
    return (Integer) getAttribute("blattnummer");
  }

  @Override
  public void setBlattnummer(Integer blattnummer) throws RemoteException
  {
    setAttribute("blattnummer", blattnummer);
  }

  @Override
  public String getName() throws RemoteException
  {
    return (String) getAttribute("name");
  }

  @Override
  public void setName(String name) throws RemoteException
  {
    setAttribute("name", name);
  }

  @Override
  public double getBetrag() throws RemoteException
  {
    Double d = (Double) getAttribute("betrag");
    if (d == null)
      return 0;
    return d.doubleValue();
  }

  @Override
  public void setBetrag(double d) throws RemoteException
  {
    setAttribute("betrag", new Double(d));
  }

  @Override
  public String getZweck() throws RemoteException
  {
    return (String) getAttribute("zweck");
  }

  @Override
  public void setZweck(String zweck) throws RemoteException
  {
    setAttribute("zweck", zweck);
  }

  @Override
  public Date getDatum() throws RemoteException
  {
    return (Date) getAttribute("datum");
  }

  @Override
  public void setDatum(Date datum) throws RemoteException
  {
    setAttribute("datum", datum);
  }

  public void setDatum(String datum) throws RemoteException
  {
    setAttribute("datum", toDate(datum));
  }

  @Override
  public String getArt() throws RemoteException
  {
    return (String) getAttribute("art");
  }

  @Override
  public void setArt(String art) throws RemoteException
  {
    setAttribute("art", art);
  }

  @Override
  public String getKommentar() throws RemoteException
  {
    return (String) getAttribute("kommentar");
  }

  @Override
  public void setKommentar(String kommentar) throws RemoteException
  {
    setAttribute("kommentar", kommentar);
  }

  @Override
  public Buchungsart getBuchungsart() throws RemoteException
  {
    Integer i = (Integer) super.getAttribute("buchungsart");
    if (i == null)
      return null; // Keine Buchungsart zugeordnet

    Cache cache = Cache.get(Buchungsart.class, true);
    return (Buchungsart) cache.get(i);
  }

  @Override
  public int getBuchungsartId() throws RemoteException
  {
    return Integer.parseInt(getBuchungsart().getID());
  }

  @Override
  public void setBuchungsart(Integer buchungsart) throws RemoteException
  {
    setAttribute("buchungsart", buchungsart);
  }

  @Override
  public Abrechnungslauf getAbrechnungslauf() throws RemoteException
  {
    return (Abrechnungslauf) getAttribute("abrechnungslauf");
  }

  @Override
  public int getAbrechnungslaufID() throws RemoteException
  {
    return Integer.parseInt(getAbrechnungslauf().getID());
  }

  @Override
  public void setAbrechnungslauf(Integer abrechnungslauf)
      throws RemoteException
  {
    setAttribute("abrechnungslauf", abrechnungslauf);
  }

  @Override
  public void setAbrechnungslauf(Abrechnungslauf abrechnungslauf)
      throws RemoteException
  {
    setAttribute("abrechnungslauf", new Integer(abrechnungslauf.getID()));
  }

  @Override
  public Mitgliedskonto getMitgliedskonto() throws RemoteException
  {
    return (Mitgliedskonto) getAttribute("mitgliedskonto");
  }

  @Override
  public int getMitgliedskontoID() throws RemoteException
  {
    return Integer.parseInt(getMitgliedskonto().getID());
  }

  @Override
  public void setMitgliedskontoID(Integer mitgliedskontoID)
      throws RemoteException
  {
    setAttribute("mitgliedskonto", mitgliedskontoID);
  }

  @Override
  public void setMitgliedskonto(Mitgliedskonto mitgliedskonto)
      throws RemoteException
  {
    if (mitgliedskonto != null)
    {
      setAttribute("mitgliedskonto", new Integer(mitgliedskonto.getID()));
    }
    else
    {
      setAttribute("mitgliedskonto", null);
    }
  }

  @Override
  public Projekt getProjekt() throws RemoteException
  {
    return (Projekt) getAttribute("projekt");
  }

  @Override
  public int getProjektID() throws RemoteException
  {
    return Integer.parseInt(getProjekt().getID());
  }

  @Override
  public void setProjektID(Integer projektID) throws RemoteException
  {
    setAttribute("projekt", projektID);
  }

  @Override
  public void setProjekt(Projekt projekt) throws RemoteException
  {
    if (projekt != null)
    {
      setAttribute("projekt", new Integer(projekt.getID()));
    }
    else
    {
      setAttribute("projekt", null);
    }
  }

  @Override
  public Spendenbescheinigung getSpendenbescheinigung() throws RemoteException
  {
    return (Spendenbescheinigung) getAttribute("spendenbescheinigung");
  }

  @Override
  public void setSpendenbescheinigungId(Integer spendenbescheinigung)
      throws RemoteException
  {
    setAttribute("spendenbescheinigung", spendenbescheinigung);
  }

  @Override
  public Map<String, Object> getMap(Map<String, Object> inma)
      throws RemoteException
  {
    Map<String, Object> map = null;
    if (inma == null)
    {
      map = new HashMap<String, Object>();
    }
    else
    {
      map = inma;
    }
    if (this.getID() == null)
    {
    }
    map.put(BuchungVar.ABRECHNUNGSLAUF.getName(),
        (this.getAbrechnungslauf() != null ? this.getAbrechnungslauf()
            .getDatum() : ""));
    map.put(BuchungVar.ART.getName(), StringTool.toNotNullString(this.getArt()));
    map.put(BuchungVar.AUSZUGSNUMMER.getName(), this.getAuszugsnummer());
    map.put(BuchungVar.BETRAG.getName(), this.getBetrag());
    map.put(BuchungVar.BLATTNUMMER.getName(), this.getBlattnummer());
    if (this.getBuchungsart() != null)
    {
      map.put(BuchungVar.BUCHUNGSARBEZEICHNUNG.getName(), this.getBuchungsart()
          .getBezeichnung());
      map.put(BuchungVar.BUCHUNGSARTNUMMER.getName(), this.getBuchungsart()
          .getNummer());
      if (this.getBuchungsart().getBuchungsklasse() != null)
      {
        map.put(BuchungVar.BUCHUNGSKLASSEBEZEICHNUNG.getName(), this
            .getBuchungsart().getBuchungsklasse().getBezeichnung());
        map.put(BuchungVar.BUCHUNGSKLASSENUMMER.getName(), this
            .getBuchungsart().getBuchungsklasse().getNummer());
      }
      else
      {
        map.put(BuchungVar.BUCHUNGSKLASSEBEZEICHNUNG.getName(), "");
        map.put(BuchungVar.BUCHUNGSKLASSENUMMER.getName(), "");
      }
    }
    else
    {
      map.put(BuchungVar.BUCHUNGSARBEZEICHNUNG.getName(), "");
      map.put(BuchungVar.BUCHUNGSARTNUMMER.getName(), "");
      map.put(BuchungVar.BUCHUNGSKLASSEBEZEICHNUNG.getName(), "");
      map.put(BuchungVar.BUCHUNGSKLASSENUMMER.getName(), "");
    }

    if (this.getProjekt() != null)
    {
      map.put(BuchungVar.PROJEKTNUMMER.getName(), this.getProjektID());
      map.put(BuchungVar.PROJEKTBEZEICHNUNG.getName(), this.getProjekt()
          .getBezeichnung());
    }
    else
    {
      map.put(BuchungVar.PROJEKTNUMMER.getName(), "");
      map.put(BuchungVar.PROJEKTBEZEICHNUNG.getName(), "");
    }

    map.put(BuchungVar.DATUM.getName(), this.getDatum());
    map.put(BuchungVar.JAHRESABSCHLUSS.getName(),
        this.getJahresabschluss() != null ? this.getJahresabschluss().getBis()
            : "");
    map.put(BuchungVar.KOMMENTAR.getName(),
        StringTool.toNotNullString(this.getKommentar()));
    map.put(BuchungVar.KONTONUMMER.getName(), this.getKonto() != null ? this
        .getKonto().getNummer() : "");
    map.put(BuchungVar.MITGLIEDSKONTO.getName(),
        this.getMitgliedskonto() != null ? this.getMitgliedskonto()
            .getMitglied().getNameVorname() : "");
    map.put(BuchungVar.NAME.getName(), this.getName());
    map.put(BuchungVar.ZWECK1.getName(),
        StringTool.toNotNullString(this.getZweck()));
    return map;
  }

  @Override
  public Object getAttribute(String fieldName) throws RemoteException
  {
    if ("id-int".equals(fieldName))
    {
      try
      {
        return new Integer(getID());
      }
      catch (Exception e)
      {
        Logger.error("unable to parse id: " + getID());
        return getID();
      }
    }

    if ("buchungsart".equals(fieldName))
      return getBuchungsart();

    if ("konto".equals(fieldName))
      return getKonto();

    return super.getAttribute(fieldName);
  }

  private Date toDate(String datum)
  {
    Date d = null;

    try
    {
      d = new JVDateFormatTTMMJJJJ().parse(datum);
    }
    catch (Exception e)
    {
      //
    }
    return d;
  }

  @Override
  public Jahresabschluss getJahresabschluss() throws RemoteException
  {
    DBIterator it = Einstellungen.getDBService().createList(
        Jahresabschluss.class);
    it.addFilter("von <= ?", new Object[] { getDatum() });
    it.addFilter("bis >= ?", new Object[] { getDatum() });
    if (it.hasNext())
    {
      Jahresabschluss ja = (Jahresabschluss) it.next();
      return ja;
    }
    return null;
  }

  @Override
  public Integer getSplitId() throws RemoteException
  {
    return (Integer) getAttribute("splitid");
  }

  @Override
  public void setSplitId(Integer splitid) throws RemoteException
  {
    setAttribute("splitid", splitid);
  }

  @Override
  public Boolean getVerzicht() throws RemoteException
  {
    return Util.getBoolean(getAttribute("verzicht"));
  }

  @Override
  public void setVerzicht(Boolean verzicht) throws RemoteException
  {
    setAttribute("verzicht", verzicht);
  }

}
