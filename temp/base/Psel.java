
import com.jsortware.jn.base.base;
import com.jsortware.jn.base.note;
import com.jsortware.jn.base.term;
import com.jsortware.jn.base.psel;
import com.jsortware.jn.base.proj;
import com.jsortware.jn.base.svr;
import com.jsortware.jn.base.state;
import com.jsortware.jn.base.recent;

// !!! allow for empty folders/recent - so projbrowse ...

List<int> Pxywh;


Psel::Psel()
{
  QSplitter *s,*t;
  QHBoxLayout h=new QHBoxLayout();
  QVBoxLayout v=new QVBoxLayout();

  s=new QSplitter();
  t=new QSplitter();

  f = panel("Folder:");
  p = panel("Project:");
  r = panel("Recent:");

  s.addWidget(f.parentWidget());
  s.addWidget(p.parentWidget());
  t.addWidget(s);
  t.addWidget(r.parentWidget());

  v=new QVBoxLayout();
  v.setContentsMargins(0,7,0,0);
  v.addWidget(t);
  h=new QHBoxLayout();
  h.setContentsMargins(0,0,10,4);
  h.addStretch(1);
  QPushButton *b=new QPushButton("Open");
  h.addWidget(b);
  v.addLayout(h);

  setLayout (v);
  setWindowTitle("Open Project");

  if (Pxywh.isEmpty())
    Pxywh << -1 << -1 << 600 << 600;

#ifdef SMALL_SCREEN
  move(0,0);
  resize(term.width(),term.height());
#else
  winpos_set(this,Pxywh);
#endif

  init();
  initsel();
  initwrite();
  prefresh();

  connect(f, SIGNAL(currentRowChanged(int)),
          this, SLOT(fsel_changed(int)));
  connect(p, SIGNAL(currentRowChanged(int)),
          this, SLOT(psel_changed(int)));
  connect(r, SIGNAL(currentRowChanged(int)),
          this, SLOT(rsel_changed(int)));

  connect(f, SIGNAL(itemActivated(ListWidgetItem *)),
          this, SLOT(itemActivated()));
  connect(p, SIGNAL(itemActivated(ListWidgetItem *)),
          this, SLOT(itemActivated()));
  connect(r, SIGNAL(itemActivated(ListWidgetItem *)),
          this, SLOT(itemActivated()));
  connect(b, SIGNAL(released()),
          this, SLOT(itemActivated()));

  exec();
}

// ---------------------------------------------------------------------
void Psel::fsel_changed(int row)
{
  if (NoEvents || row == Ftx) return;
  noevents(1);
  Ftx=row;
  getids(Ftx);
  prefresh();
  getrecentx();
  r.setCurrentRow(Rtx);
  noevents(0);
}

// ---------------------------------------------------------------------
void Psel::getids(int n)
{
  if (Ids.at(n).size()) return;

  int i,ind,ndx;
  ndx=-1;
  String[] p=project_tree(Paths.at(n));
  for (i=0; i<p.size(); i++)
    if (p.at(i).isEmpty()) p.replace(i,"{root}");
  Ids.replace(n,p);
  if (p.size()>0 && RecentFolders.size()>0) {
    ind=RecentFolders.indexOf(Folders.at(Ftx));
    if (ind >= 0) ndx=p.indexOf(RecentProjects.at(ind));
  }
  if (p.size()==1) ndx=0;
  Idx.replace(Ftx,ndx);
}

// ---------------------------------------------------------------------
String[] Psel::getrecent()
{
  String[] r;
  String s;
  for (int i=0; i<recent.Projects.size(); i++) {
    s = recent.Projects.at(i).at(0);
    if (s.size()) r.append(s);
  }
  return r;
}

// ---------------------------------------------------------------------
void Psel::getrecentx()
{
  int i,ndx;

  Rtx = -1;
  if (Ftx<0) return;

  ndx=Idx.at(Ftx);
  if (ndx<0) return;
  String fx=Folders.at(Ftx);
  String px=Ids.at(Ftx).at(ndx);

  for (i=0; i<Recent.size(); i++)
    if (RecentFolders.at(i)==fx && RecentProjects.at(i)==px) break;

  Rtx=(i<Recent.size()) ? i : -1;
}

// ---------------------------------------------------------------------
void Psel::init()
{
  int i,n;
  String s;
  String[] t;
  Folders=config.UserFolderKeys;
  Paths=config.UserFolderValues;
  for (i=0; i<Folders.size(); i++) {
    Ids.append(t);
    Idx.append(-1);
  }
  Recent=getrecent();
  for (i=0; i<Recent.size(); i++) {
    s = Recent.at(i);
    if (isroot(s)) {
      RecentFolders.append("");
      RecentProjects.append(s);
    } else {
      n=s.indexOf('/');
      RecentFolders.append(s.left(n));
      RecentProjects.append(s.mid(n+1));
    }
  }
}

// ---------------------------------------------------------------------
void Psel::initsel()
{
  int ndx;
  String[] t;
  if (Recent.size()) {
    Rtx=0;
    Ftx=Folders.indexOf(RecentFolders.at(0));
    if (Ftx >= 0) {
      getids(Ftx);
      t=Ids.at(Ftx);
      ndx = t.indexOf(RecentProjects.at(0));
      Idx.replace(Ftx,ndx);
    }
  } else {
    Rtx=-1;
    Ftx=qMax(0,Folders.indexOf(project.Folder));
    getids(Ftx);
    t=Ids.at(Ftx);

    ndx=t.indexOf(project.Path);
    Idx.replace(Ftx,ndx);
  }
}

// ---------------------------------------------------------------------
void Psel::initwrite()
{
  f.addItems(Folders);
  r.addItems(Recent);
  f.setCurrentRow(Ftx);
  r.setCurrentRow(Rtx);
}

// ---------------------------------------------------------------------
void Psel::itemActivated()
{
  String id;

  if (Rtx >= 0)
    id=Recent.at(Rtx);
  else {
    if (Ftx<0 || Idx.at(Ftx)<0) {
      info("Project","No project selected");
      return;
    }
    id=Folders.at(Ftx) + "/" + Ids.at(Ftx).at(Idx.at(Ftx));
  }

  if (note == 0)
    term.vieweditor();
  else
    note.projectsave();
  project.open(id);
  note.projectopen(true);
  close();
}

// ---------------------------------------------------------------------
ListWidget *Psel::panel(String s)
{
  ListWidget *f = new ListWidget();
  QLabel *a=new QLabel(s);
  QWidget *w=new QWidget();
  QVBoxLayout v=new QVBoxLayout();
  v.setContentsMargins(0,0,0,0);
  v.addWidget(a);
  v.addWidget(f);
  w.setLayout(v);
  return f;
}

// ---------------------------------------------------------------------
void Psel::prefresh()
{
  p.clear();
  if (Ftx>=0) {
    p.addItems(Ids.at(Ftx));
    p.setCurrentRow(Idx.at(Ftx));
  }
}

// ---------------------------------------------------------------------
void Psel::psel_changed(int row)
{
  if (NoEvents || row == Idx.at(Ftx)) return;
  noevents(1);
  Idx.replace(Ftx,row);
  getrecentx();
  r.setCurrentRow(Rtx);
  noevents(0);
}

// ---------------------------------------------------------------------
void Psel::reject()
{
  Pxywh=winpos_get(this);
  QDialog::reject();
}

// ---------------------------------------------------------------------
void Psel::keyReleaseEvent(QKeyEvent *event)
{
#ifdef QT_OS_ANDROID
  if (event.key()==Qt::Key_Back) {
    reject();
  } else QDialog::keyReleaseEvent(event);
#else
  QDialog::keyReleaseEvent(event);
#endif
}

// ---------------------------------------------------------------------
void Psel::rsel_changed(int row)
{
  if (NoEvents || row==Rtx) return;
  noevents(1);
  int ndx;
  String[] t;
  Rtx=row;
  Ftx=Folders.indexOf(RecentFolders.at(Rtx));
  if (Ftx>=0) {
    getids(Ftx);
    t=Ids.at(Ftx);
    ndx=t.indexOf(RecentProjects.at(Rtx));
    Idx.replace(Ftx,ndx);
    f.setCurrentRow(Ftx);
  }
  prefresh();
  noevents(0);
}

