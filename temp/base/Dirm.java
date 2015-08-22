
import com.jsortware.jn.base.pcombobox;
import com.jsortware.jn.base.base;
import com.jsortware.jn.base.widget;
import com.jsortware.jn.base.dirm;
import com.jsortware.jn.base.note;
import com.jsortware.jn.base.proj;
import com.jsortware.jn.base.recent;
import com.jsortware.jn.base.snap;
import com.jsortware.jn.base.state;
import com.jsortware.jn.base.term;
import com.jsortware.jn.base.view;


extern "C" {
  Dllexport void dirmatch(const char *s,const char *t);
}

// ---------------------------------------------------------------------
Dirm::Dirm(String s)
{
  Tab=s;
  Title="Directory Match";
  config.dirmatch_init();
  QVBoxLayout layout=new QVBoxLayout;
  layout.setContentsMargins(layout.contentsMargins());
  layout.setSpacing(0);
  layout.addWidget(createmenu());
  layout.addWidget(createpanel());
  layout.addWidget(createview(),1);
  setWindowTitle(Title);
  setLayout(layout);
#ifdef SMALL_SCREEN
  move(0,0);
  resize(term.width(),term.height());
#else
  setxywh(this,"Dirm");
#endif
  QMetaObject::connectSlotsByName(this);
  init();
  show();
}


// ---------------------------------------------------------------------
QMenuBar *Dirm::createmenu()
{
  fileselAct = makeact("fileselAct","Select from Favorites","");
  filequitAct = makeact("filequitAct","&Quit","Ctrl+Q");
  toswapAct = makeact("toswapAct","Swap source and target directories","");
  tocopysrcAct = makeact("tocopysrcAct","Copy source files not in target","");
  tocopylaterAct = makeact("tocopylaterAct","Copy source files later than target","");
  tocopyallAct = makeact("tocopyallAct","Copy all source files","");

  QMenuBar *r=new QMenuBar();
  QMenu *m=new (QMenu);

  m=r.addMenu("&File");
  if (Tab.equals("std"))
    m.addAction(fileselAct);
  m.addAction(filequitAct);
  m=r.addMenu("&Tools");

  if (Tab.equals("std"))
    m.addAction(toswapAct);
  m.addAction(tocopysrcAct);
  m.addAction(tocopylaterAct);
  m.addAction(tocopyallAct);

  enablefound(false);

  return r;
}

// ---------------------------------------------------------------------
QWidget *Dirm::createpanel()
{
  QWidget *w=new QWidget();
  QHBoxLayout h=new QHBoxLayout();
#ifdef SMALL_SCREEN
  h.setSpacing(0);
#else
  h.setSpacing(15);
#endif

  QFormLayout f = new QFormLayout;
  lsource = new QLabel();
  ltarget = new QLabel();
  ltype = new QLabel();
  source = makecombobox("source");
  target = makecombobox("target");
  type = makecombobox("type");
  f.setVerticalSpacing(2);
  f.addRow(lsource,source);
  f.addRow(ltarget,target);
  f.addRow(ltype,type);
  h.addLayout(f,1);

  filler=new hPushButton("Compare Select");

#ifdef SMALL_SCREEN
  subdir= makecheckbox("Include subdir","subdir");
#else
  subdir= makecheckbox("Include subdirectories","subdir");
#endif

#ifndef SMALL_SCREEN
  QVBoxLayout m=new QVBoxLayout;
  m.setSpacing(0);
  m.addWidget(subdir);
  if (Tab.equals("snp"))
    subdir.hide();
  m.addWidget(filler);
  m.addStretch(1);
  h.addLayout(m);
#endif

  QVBoxLayout v=new QVBoxLayout();
#ifdef SMALL_SCREEN
  v.addWidget(subdir);
  if (Tab.equals("snp"))
    subdir.hide();
#endif
  match=makebutton("match","Match");
  v.addWidget(match);
  v.addWidget(filler);
  v.addStretch(1);
  h.addLayout(v);

  w.setLayout(h);
  return w;
}

// ---------------------------------------------------------------------
QWidget *Dirm::createview()
{
  QWidget *w=new QWidget();
  QHBoxLayout h=new QHBoxLayout();
  h.setContentsMargins(0,0,11,0);
  found=new ListWidget;
  found.setAlternatingRowColors(true);
  found.setFont(config.Font);
  h.addWidget(found,1);

  compareall= makebutton("compareall","Compare All");
  compare= makebutton("compare","Compare Select");
  exdiff= makebutton("exdiff","External Diff");
  open= makebutton("open","Open");
  view= makebutton("view","View");
  copy= makebutton("copy","Copy");
  ignore= makebutton("ignore","Ignore");

  QVBoxLayout v=new QVBoxLayout();
  v.setSpacing(0);
  v.addWidget(compareall);
  v.addWidget(compare);
  v.addWidget(exdiff);
  v.addWidget(open);
  v.addWidget(view);
  v.addWidget(copy);
  v.addWidget(ignore);
  v.addStretch(1);
  h.addLayout(v);

  w.setLayout(h);
  return w;
}

// ---------------------------------------------------------------------
// get single full name or empty
String Dirm::dmgetname1()
{
  String r;
  String[] n=dmgetnames();
  if (n.isEmpty()) return r;

  r=n[1];
  if (r.isEmpty()) return r;

  if (r==n[0])
    return Target+"/"+r;

  if (r==n[2])
    return Source+"/"+r;

  if (NotInSource.contains(r))
    return Target+"/"+r;

  return Source+"/"+r;

}

// ---------------------------------------------------------------------
// get paired name or empty
String Dirm::dmgetname2()
{
  String r;
  String[] n=dmgetnames();
  if (n.isEmpty()) return r;
  r=n[1];
  if (r.isEmpty()) return r;
  if (r==n[0]||r==n[2]) return r;
  r.clear();
  return r;
}

// ---------------------------------------------------------------------
// get prev, current, next names
String[] Dirm::dmgetnames()
{
  String[] r;
  int n=found.currentRow();
  if (n==-1) return r;
  if (n==0)
    r.append("");
  else
    r.append(qstaketo(found.item(n-1).text()," "));
  r.append(qstaketo(found.item(n).text()," "));
  if (n==found.count()-1)
    r.append("");
  else
    r.append(qstaketo(found.item(n+1).text()," "));
  return r;
}

// ---------------------------------------------------------------------
void Dirm::dminfo(String txt)
{
  info(Title,txt);
}

// ---------------------------------------------------------------------
void Dirm::dmread()
{
  String s;
  if (Tab.equals("std")) {
    Source=cpath(source.currentText());
    Target=cpath(target.currentText());
    TypeInx=type.currentIndex();
    Subdir=subdir.isChecked();
    Sourcex=termsep(tofoldername(Source));
    Targetx=termsep(tofoldername(Target));
    dmsetdirs(Source,Target,false);
  } else {
    s=target.currentText();
    Source=SnapDir + "/" + s;
    Sourcex="~snapshot/" + s + "/";
    s=type.currentText();
    if (s.equals("Current")) {
      Target=cpath("~"+Project);
      Targetx="~" + Project + "/";
    } else {
      Target=SnapDir + "/" + s;
      Targetx="~snapshot/" + s + "/";
    }
    TypeInx=config.DMTypes.length()-1;
    Subdir=true;
  }

}

// ---------------------------------------------------------------------
void Dirm::dmsaverecent() {};

// ---------------------------------------------------------------------
// set source, target in dirs
void Dirm::dmsetdirs(String s,String t,boolean refresh)
{
  noevents(1);
  Dirs.prepend(t);
  Dirs.prepend(s);
  Dirs.removeAll("");
  Dirs.removeDuplicates();
  if(Max<Dirs.length())
    Dirs=Dirs.mid(0,Max);
  if(refresh) {
    source.clear();
    source.addItems(Dirs);
    source.setCurrentIndex(0);
    target.clear();
    target.addItems(Dirs);
    target.setCurrentIndex(1);
  }
  noevents(0);
}

// ---------------------------------------------------------------------
void Dirm::dmshowfind()
{
  found.clear();
  if(Found.length()) {
    enablefound(true);
    found.addItems(Found);
  } else {
    enablefound(false);
    dminfo("Contents match");
  }
};

// ---------------------------------------------------------------------
void Dirm::dmwrite() {};

// ---------------------------------------------------------------------
void Dirm::enablefound(boolean b)
{
  tocopyallAct.setEnabled(b);
  tocopylaterAct.setEnabled(b);
  tocopysrcAct.setEnabled(b);
}

// ---------------------------------------------------------------------
void Dirm::init()
{
  matched=0;
  written=0;
  Subdir=true;
  if (Tab.equals("std"))
    init_std();
  else
    init_snp();
}

// ---------------------------------------------------------------------
void Dirm::init_snp()
{
  String[] folders;
  lsource.setText("Project");
  ltarget.setText("Source:");
  ltype.setText("Target:");
  folders=project_tree("~"+project.Folder);
  folders=qslprependeach(project.Folder+"/",folders);
  source.addItems(folders);
  source.setCurrentIndex(folders.indexOf(project.Id));
  init_snp1(project.Id);
}

// ---------------------------------------------------------------------
void Dirm::init_snp1(String pid)
{
  String[] snaps;
  Project=pid;
  SnapDir=snappath(cpath("~"+project.Id));
  snaps=ss_list(SnapDir);
  target.clear();
  type.clear();
  if (snaps.length()==0) {
    dminfo("No snapshots for: " + project.Id);
    return;
  }
  target.addItems(snaps);
  target.setCurrentIndex(0);
  snaps.prepend("Current");
  type.addItems(snaps);
  type.setCurrentIndex(0);
}

// ---------------------------------------------------------------------
void Dirm::init_std()
{
  Dirs=config.DMFavorites;
  TypeInx=config.DMTypeIndex;
  Max=qMax(24,2*1+Dirs.length()/2);
  lsource.setText("Source:");
  ltarget.setText("Target:");
  ltype.setText("Type:");
  source.addItems(Dirs);
  if(source.count()>0) source.setCurrentIndex(0);
  target.addItems(Dirs);
  if(target.count()>1) target.setCurrentIndex(1);
  type.addItems(config.DMTypes);
  if(type.count()) type.setCurrentIndex(TypeInx);
  subdir.setChecked(Subdir);
}

// ---------------------------------------------------------------------
QAction *Dirm::makeact(String id, String text, String shortcut)
{
  QAction *r = new QAction(text,this);
  if (shortcut.length())
    r.setShortcut(shortcut);
  return r;
}

// ---------------------------------------------------------------------
QPushButton *Dirm::makebutton(String id, String text)
{
  QPushButton *r = new QPushButton(text);
  return r;
}

// ---------------------------------------------------------------------
void Dirm::on_compareall_clicked()
{
  compareallfiles();
}

// ---------------------------------------------------------------------
void Dirm::on_compare_clicked()
{
  comparefile();
}

// ---------------------------------------------------------------------
void Dirm::on_copy_clicked()
{
  copyfile();
}

// ---------------------------------------------------------------------
void Dirm::on_exdiff_clicked()
{
  comparexdiff();
}

// ---------------------------------------------------------------------
void Dirm::on_filequitAct_triggered()
{
  savepos();
  close();
}

// ---------------------------------------------------------------------
void Dirm::on_fileselAct_triggered()
{
  new Favs(this);
}

// ---------------------------------------------------------------------
void Dirm::on_ignore_clicked()
{
  ignorefile();
}

// ---------------------------------------------------------------------
void Dirm::on_match_clicked()
{
  matches(0);
  noevents(1);
  dmshowfind();
  dmsaverecent();
  dmwrite();
  matched=true;
  noevents(0);
}

// ---------------------------------------------------------------------
void Dirm::on_open_clicked()
{
  String s=dmgetname1();
  if (s.isEmpty()) {
    dminfo("No file selected");
    return;
  }
  term.vieweditor();
  note.fileopen(s);
}

// ---------------------------------------------------------------------
void Dirm::on_source_currentIndexChanged()
{
  int n;
  if(NoEvents) return;
  if (Tab.equals("std")) {
    if (source.currentText()==target.currentText()) {
      n=target.currentIndex();
      n=(n==0)?n+1:n-1;
      target.setCurrentIndex(n);
    }
    match_refresh(true);
  } else
    init_snp1(source.currentText());

};

// ---------------------------------------------------------------------
void Dirm::on_subdir_stateChanged()
{
  Subdir=subdir.isChecked();
  if(NoEvents) return;
  match_refresh(1);
}

// ---------------------------------------------------------------------
void Dirm::on_target_currentIndexChanged()
{
  int n;
  if(NoEvents) return;
  if (Tab.equals("std")) {
    if (source.currentText()==target.currentText()) {
      n=source.currentIndex();
      n=(n==0)?n+1:n-1;
      source.setCurrentIndex(n);
    }
  } else if (type.currentText()==target.currentText()) {
    n=type.currentIndex();
    n=(n==0)?n+1:n-1;
    type.setCurrentIndex(n);
  }
  match_refresh(true);
};


// ---------------------------------------------------------------------
void Dirm::on_tocopysrcAct_triggered()
{
  copysource();
}

// ---------------------------------------------------------------------
void Dirm::on_tocopylaterAct_triggered()
{
  copylater();
}

// ---------------------------------------------------------------------
void Dirm::on_tocopyallAct_triggered()
{
  copyall();
}

// ---------------------------------------------------------------------
void Dirm::on_toswapAct_triggered()
{
  noevents(1);
  dmread();
  String s=Dirs[0];
  dmsetdirs(Target,Source,true);
  match_refresh(true);
  noevents(0);
}

// ---------------------------------------------------------------------
void Dirm::on_type_currentIndexChanged()
{
  int n;
  if(NoEvents) return;
  if (Tab.equals("snp")) {
    if (type.currentText()==target.currentText()) {
      n=target.currentIndex();
      n=(n==0)?n+1:n-1;
      target.setCurrentIndex(n);
    }
  }

  match_refresh(true);
};

// ---------------------------------------------------------------------
void Dirm::on_view_clicked()
{
  String r,s;
  r=s=dmgetname1();
  if (s.isEmpty()) {
    dminfo("No file selected");
    return;
  }

  if (Tab.equals("snp") && matchhead(Source,s))
    r="snap: " + s.mid(38+s.indexOf(".snp"));

  textview(tofoldername(r),cfread(s));
}

// ---------------------------------------------------------------------
void Dirm::reject()
{
  savepos();
  QDialog::reject();
}

// ---------------------------------------------------------------------
void Dirm::savepos()
{
  config.winpos_save(this,"Dirm");
}

// ---------------------------------------------------------------------
Favs::Favs(Dirm *d)
{
  int i,rws;
  String s;
  QTableWidget *w=new QTableWidget;
  QTableWidgetItem *c;

  dirm=d;
  wfav=w;

  rws=config.DMFavorites.length()/2;
  if (rws==0) {
    s="No favorites defined.\n\n";
    info("Favorites",s+"See menu Edit|Configure|Directory Match.");
    return;
  }

  w.setRowCount(rws);
  w.setColumnCount(2);

  c=new QTableWidgetItem();
  c.setText("Source");
  c.setTextAlignment(Qt::AlignLeft);
  w.setHorizontalHeaderItem(0,c);
  c=new QTableWidgetItem();
  c.setText("Target");
  c.setTextAlignment(Qt::AlignLeft);
  w.setHorizontalHeaderItem(1,c);

  for(i=0; i<rws; i++) {
    c=new QTableWidgetItem();
    c.setText(config.DMFavorites[2*i]);
    w.setItem(i,0,c);
    c=new QTableWidgetItem();
    c.setText(config.DMFavorites[1+2*i]);
    w.setItem(i,1,c);
  }

  QVBoxLayout b = new QVBoxLayout;
  b.setContentsMargins(0,0,0,0);
  b.addWidget(w);
  setLayout(b);
#ifndef SMALL_SCREEN
  resize(600,300);
#endif
  setWindowTitle("Directory Match Favorites");

  w.resizeColumnsToContents();
  w.setAlternatingRowColors(true);
#ifndef QT50
  w.horizontalHeader().setResizeMode(QHeaderView::Stretch);
#endif
  w.verticalHeader().setVisible(false);
  w.setSelectionBehavior(QAbstractItemView::SelectRows);
  w.setSelectionMode(QAbstractItemView::SingleSelection);
  w.setEditTriggers(QAbstractItemView::NoEditTriggers);

  QFontMetrics fm(w.font());
  w.verticalHeader().setDefaultSectionSize(fm.height() + 6);

  connect(w,SIGNAL(cellActivated(int,int)),
          this,SLOT(cellActivated(int)));
  exec();
};

// ---------------------------------------------------------------------
void Favs::cellActivated(int row)
{
  String s=wfav.item(row,0).text();
  String t=wfav.item(row,1).text();
  dirm.dmsetdirs(s,t,true);
  close();
}

// ---------------------------------------------------------------------
void dirmatch(const char *s,const char *t)
{
  Dirm *d=new Dirm("std");
  if (strlen(s)) {
    d.source.insertItem(0,c2q(s));
    d.source.setCurrentIndex(0);
  }
  if (strlen(t)) {
    d.target.insertItem(0,c2q(t));
    d.target.setCurrentIndex(0);
  }
}
