package de.jost_net.JVerein.gui.parts;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import de.jost_net.JVerein.Einstellungen;
import de.jost_net.JVerein.JVereinPlugin;
import de.jost_net.JVerein.gui.action.DokumentationAction;
import de.jost_net.JVerein.gui.view.DokumentationUtil;
import de.jost_net.JVerein.gui.view.LesefeldDetailView;
import de.jost_net.JVerein.rmi.Lesefeld;
import de.jost_net.JVerein.rmi.Mitglied;
import de.jost_net.JVerein.util.LesefeldAuswerter;
import de.willuhn.datasource.rmi.DBIterator;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.Part;
import de.willuhn.jameica.gui.dialogs.AbstractDialog;
import de.willuhn.jameica.gui.dialogs.YesNoDialog;
import de.willuhn.jameica.gui.input.SelectInput;
import de.willuhn.jameica.gui.input.TextAreaInput;
import de.willuhn.jameica.gui.parts.Button;
import de.willuhn.jameica.gui.parts.ButtonArea;
import de.willuhn.jameica.gui.util.ColumnLayout;
import de.willuhn.jameica.gui.util.ScrolledContainer;
import de.willuhn.jameica.gui.util.SimpleContainer;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

/**
 * Listet Namen der definierten Lesefelder und zeigt der Inhalt dieser
 * Lesefelder f�r ein ausw�hlebares Mitglied an. F�r jedes Lesefeld wird ein
 * Bearbeiten- und L�schen-Knopf angezeigt. Als Part implementiert um es ggf.
 * einfach verschieben zu k�nnen.
 * 
 * @author Julian
 */
public class LesefeldUebersichtPart implements Part
{
  private ColumnLayout lesefelderListeLayout;

  private Mitglied selectedMitglied;

  private LesefeldAuswerter lesefeldAuswerter;

  /**
   * Mit selectedMitglied kann ein beliebiges Mitglied in der GUI ausgw�hlt
   * werden. Ist selectedMitglied==null, wird das erste gefunde Mitglied
   * ausgew�hlt.
   * 
   * @param selectedMitglied
   *          Auszuw�hlendes Mitglied.
   */
  public LesefeldUebersichtPart(Mitglied selectedMitglied)
  {
    this.selectedMitglied = selectedMitglied;
  }

  @Override
  public void paint(final Composite parent) throws RemoteException
  {
    ScrolledContainer scrolled = new ScrolledContainer(parent, 1);

    SimpleContainer container = new SimpleContainer(scrolled.getComposite());

    container.addHeadline(JVereinPlugin.getI18n().tr("Lesefelder"));

    // Hole alle Mitglieder aus Datenbank um sie sp�ter anzuzeigen.
    DBIterator it = Einstellungen.getDBService().createList(Mitglied.class);
    // optional k�nnten Filter eingebaut werden:
    // it.addFilter("plz='" + (String) plz.getValue() + "'");
    ArrayList<Mitglied> mitgliederList = new ArrayList<Mitglied>();
    while (it.hasNext())
    {
      mitgliederList.add((Mitglied) it.next());
    }

    // Das erste Mitglied wird ausgew�hlt, wenn im Contructor keins gew�hlt
    // wurde.
    if (selectedMitglied == null)
      selectedMitglied = mitgliederList.get(0);
    if (selectedMitglied == null)
    {
      Label textLabel = new Label(container.getComposite(), SWT.NONE);
      textLabel.setText(JVereinPlugin.getI18n().tr(
          "Bitte erst ein Mitglied anlegen."));
      return;
    }

    // MITGLIEDER AUSWAHL-FELD
    final SelectInput mitgliederSelectInput = new SelectInput(
        mitgliederList.toArray(), selectedMitglied);

    mitgliederSelectInput.addListener(new Listener()
    {
      @Override
      public void handleEvent(Event event)
      {
        Mitglied selected = (Mitglied) mitgliederSelectInput.getValue();
        if (selected == null || selected == selectedMitglied)
          return;

        selectedMitglied = selected;
        try
        {
          lesefeldAuswerter.setMap(selectedMitglied.getMap(null, true));
          lesefeldAuswerter.evalAlleLesefelder();
          List<Lesefeld> lesefelder = lesefeldAuswerter.getLesefelder();
          for (Lesefeld lesefeld : lesefelder)
          {
            updateLesefeldEinstellungRow(lesefeld);
          }
        }
        catch (RemoteException e)
        {
          String fehler = JVereinPlugin.getI18n().tr(
              "Fehler beim Ausw�hlen des Mitgliedes");
          Logger.error(fehler, e);
          GUI.getStatusBar().setErrorText(fehler);
          e.printStackTrace();
        }

      }
    });
    mitgliederSelectInput.setName(JVereinPlugin.getI18n().tr("Mitgliedschaft"));
    container.addLabelPair(JVereinPlugin.getI18n().tr("Mitglied"),
        mitgliederSelectInput);
    // ENDE MITGLIEDER AUSWAHL-FELD

    // LesefelderListeLayout
    // darf nur �ber die Funktionen
    // addLesefeldEinstellungRow(), updateLesefeldEinstellungRow()
    // und deleteLesefeldEinstellungRow()
    // manipuliert werden.
    lesefelderListeLayout = new ColumnLayout(container.getComposite(), 4);

    lesefeldAuswerter = new LesefeldAuswerter();
    lesefeldAuswerter.setLesefelderDefinitionsFromDatabase();
    lesefeldAuswerter.setMap(selectedMitglied.getMap(null, true));
    lesefeldAuswerter.evalAlleLesefelder();
    List<Lesefeld> lesefelder = lesefeldAuswerter.getLesefelder();
    for (Lesefeld lesefeld : lesefelder)
    {
      addLesefeldEinstellungRow(lesefeld);
    }

    // BUTTON AREA
    ButtonArea buttons = new ButtonArea();
    buttons.addButton(JVereinPlugin.getI18n().tr("Hilfe"),
        new DokumentationAction(), DokumentationUtil.LESEFELDER, false,
        "help-browser.png");
    buttons.addButton(JVereinPlugin.getI18n().tr("neu"),
        new NewLesefeldAction(), null, false, "document-new.png");
    buttons.paint(parent);
    // END BUTTON AREA
  }

  /**
   * F�gt neue GUI-Elemente hinzu f�r ein neues Lesefeld.
   * 
   * @param lesefeld
   *          Anzuzeigendes, neues Lesefeld.
   * @throws RemoteException
   */
  private void addLesefeldEinstellungRow(final Lesefeld lesefeld)
      throws RemoteException
  {
    // Lesefeld Beschreibung
    final Label label = GUI.getStyleFactory().createLabel(
        lesefelderListeLayout.getComposite(), SWT.NONE);
    label.setText(lesefeld.getBezeichnung());

    // Lesefeld Inhalt
    TextAreaInput tt = new TextAreaInput(lesefeld.getEvaluatedContent());
    tt.setEnabled(false);
    lesefelderListeLayout.add(tt);

    // Bearbeiten-Button
    Button button = new Button(JVereinPlugin.getI18n().tr("bearbeiten"),
        new EditLesefeldAction(lesefeld), null, false, "edit.png");
    lesefelderListeLayout.add(button);

    // L�schen-Button
    button = new Button(JVereinPlugin.getI18n().tr("l�schen"),
        new DeleteLesefeldAction(lesefeld), null, false, "list-remove.png");
    lesefelderListeLayout.add(button);
  }

  /**
   * Aktualisiert die GUI, so dass Inhalt von lese angezeigt wird.
   * 
   * @param lesefeld
   *          Anzuzeigendes, neues Lesefeld.
   * @throws RemoteException
   */
  private void updateLesefeldEinstellungRow(final Lesefeld lesefeld)
      throws RemoteException
  {
    boolean updateThis = false;
    for (Control child : lesefelderListeLayout.getComposite().getChildren())
    {
      if (child instanceof Label)
      {
        Label label = (Label) child;
        if (label.getText().equals(lesefeld.getBezeichnung()))
        {
          updateThis = true;
        }
        else
          updateThis = false;
      }

      if (updateThis)
      {
        if (child instanceof Composite)
        {
          Composite c = (Composite) child;
          for (Control child2 : c.getChildren())
          {
            if (child2 instanceof org.eclipse.swt.widgets.Text)
            {
              org.eclipse.swt.widgets.Text t = (org.eclipse.swt.widgets.Text) child2;
              t.setText(lesefeld.getEvaluatedContent());
            }
          }
        }
      }
    }
    updateView();
  }

  /**
   * L�scht GUI-Elemente von Lesefeld lf, die von addLesefeldEinstellungRow()
   * erzeugt wurden.
   * 
   * @param lf
   *          Zu l�schendes Lesefeld.
   * @throws RemoteException
   */
  private void deleteLesefeldEinstellungRow(Lesefeld lf) throws RemoteException
  {
    boolean deleteThis = false;
    lesefelderListeLayout.getComposite().getChildren();
    for (Control child : lesefelderListeLayout.getComposite().getChildren())
    {
      if (child instanceof Label)
      {
        Label label = (Label) child;

        // Solange das Lesefeld noch nicht in DB gespeichert wurde, besitzt lf
        // noch keine eindeutige ID.
        // Nutze daher die Bezeichnung. Die Eindeutigkeit von von der GUI
        // sichergestellt.
        if (label.getText().equals(lf.getBezeichnung()))
        {
          deleteThis = true;
        }
        else
          deleteThis = false;
      }

      if (deleteThis)
      {
        child.dispose();
      }
    }

    updateView();

  }

  /**
   * Veranlasst das Neu-Zeichen (inklusive Aktualisieren des Inhaltes,
   * Gr��enanpassung und Ausrichtung) der GUI-Elemente f�r die Lesefelder.
   */
  private void updateView()
  {

    Point currentSizeParentParent = lesefelderListeLayout.getComposite()
        .getParent().getParent().getSize();
    Point sizeParentParent = lesefelderListeLayout.getComposite().getParent()
        .computeSize(currentSizeParentParent.x, SWT.DEFAULT, true);
    lesefelderListeLayout.getComposite().getParent().getParent()
        .setSize(sizeParentParent);

    Point currentSizeParent = lesefelderListeLayout.getComposite().getParent()
        .getSize();
    Point sizeParent = lesefelderListeLayout.getComposite().getParent()
        .computeSize(currentSizeParent.x, SWT.DEFAULT, true);
    lesefelderListeLayout.getComposite().getParent().setSize(sizeParent);

    lesefelderListeLayout.getComposite().redraw();
    lesefelderListeLayout.getComposite().update();
    lesefelderListeLayout.getComposite().layout();

    lesefelderListeLayout.getComposite().getParent().redraw();
    lesefelderListeLayout.getComposite().getParent().update();
    lesefelderListeLayout.getComposite().getParent().layout();
  }

  class NewLesefeldAction implements Action
  {
    @Override
    public void handleAction(Object context) throws ApplicationException
    {
      openEditLesefeldDialog(null);
    }
  }

  class EditLesefeldAction implements Action
  {
    Lesefeld lesefeld;

    public EditLesefeldAction(Lesefeld lesefeld)
    {
      this.lesefeld = lesefeld;
    }

    @Override
    public void handleAction(Object context) throws ApplicationException
    {
      openEditLesefeldDialog(lesefeld);
    }
  }

  /**
   * �ffnet eine neue View zum Editieren einer Lesefeld-Definition.
   * 
   * @param lesefeld
   *          Zu bearbeitendes Lesefeld oder null, wenn diese Lesefeld angelegt
   *          werden soll.
   * @throws ApplicationException
   */
  private void openEditLesefeldDialog(Lesefeld lesefeld)
      throws ApplicationException
  {

    GUI.startView(new LesefeldDetailView(lesefeldAuswerter, lesefeld), null);
  }

  class DeleteLesefeldAction implements Action
  {
    Lesefeld lesefeld;

    public DeleteLesefeldAction(Lesefeld lesefeld)
    {
      this.lesefeld = lesefeld;
    }

    @Override
    public void handleAction(Object context) throws ApplicationException
    {
      /* Sicherheitsnachfrage */
      YesNoDialog ynd = new YesNoDialog(AbstractDialog.POSITION_CENTER);
      ynd.setText(JVereinPlugin.getI18n().tr(
          "Achtung! Lesefeld wird gel�scht. Weiter?"));
      ynd.setTitle(JVereinPlugin.getI18n().tr("L�schen"));
      Boolean choice;
      try
      {
        choice = (Boolean) ynd.open();
        if (!choice.booleanValue())
          return;
      }
      catch (Exception e1)
      {
        e1.printStackTrace();
      }

      try
      {
        deleteLesefeldEinstellungRow(lesefeld);
        lesefeldAuswerter.deleteLesefelderDefinition(lesefeld);
        lesefeld.delete();
      }
      catch (RemoteException e)
      {
        String fehler = JVereinPlugin.getI18n().tr(
            "Fehler beim L�schen des Lesefeldes");
        Logger.error(fehler, e);
        GUI.getStatusBar().setErrorText(fehler);
        e.printStackTrace();
      }
    }

  }

}
