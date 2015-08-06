

import com.jsoftware.jn.wd.isigraph;
import com.jsoftware.jn.wd.isigraph2;
import com.jsoftware.jn.wd.form;
import com.jsoftware.jn.wd.pane;

extern Child isigraph;
extern "C" int glclear2 (void *p,int clear);

// ---------------------------------------------------------------------
Isigraph2::Isigraph2(Child c, View parent) : View(parent)
{
  Q_UNUSED(parent);
  pchild = c;
  type=pchild.type;
  painter=0;
  font=0;
  if (type.equals("isigraph"))
    pixmap=0;
  else {
    pixmap=new QPixmap(1,1);
    pixmap.fill(QColor(0,0,0,0));
    painter=new QPainter(pixmap);
    painter.setRenderHint(QPainter::Antialiasing, true);
  }
  glclear2(this,0);
  this.setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Expanding);
  setMouseTracking(true);           // for mmove event
  setFocusPolicy(Qt::StrongFocus);  // for char event
}

// ---------------------------------------------------------------------
Isigraph2::~Isigraph2()
{
  if (pchild==pchild.pform.isigraph)
    pchild.pform.isigraph=0;
  if (painter) {
    painter.end();
    delete painter;
    painter=0;
  }
  if (pixmap) delete pixmap;
  pixmap=0;
}

// ---------------------------------------------------------------------
void Isigraph2::fill(const int *p)
{
  QColor c(*(p), *(p + 1), *(p + 2), *(p + 3));
  if (pixmap)
    pixmap.fill(c);
  else
    painter.fillRect(0,0,width(),height(),c);
}

// ---------------------------------------------------------------------
QPixmap Isigraph2::getpixmap()
{
  QPixmap m;
  if (pixmap && !pixmap.isNull())
    return pixmap.copy(0,0,width(),height());
  if (painter) return m;
  QPixmap p(size());
  render(&p);
  return p;
}

// ---------------------------------------------------------------------
void Isigraph2::paintEvent(QPaintEvent *event)
{
  Q_UNUSED(event);
  if (type.equals("isidraw"))
    paintEvent_isidraw();
  else
    paintEvent_isigraph();
}

// ---------------------------------------------------------------------
void Isigraph2::paintEvent_isidraw()
{
  if (!pixmap || pixmap.isNull()) return;
  QPainter p(this);
  p.drawPixmap(0,0,*pixmap,0,0,width(),height());
}

// ---------------------------------------------------------------------
void Isigraph2::paintEvent_isigraph()
{
  if (painter) return;
  form=pchild.pform;
  form.isigraph=(Child ) this;
  painter=new QPainter(this);
  if (painter.isActive()) painter.setRenderHint(QPainter::Antialiasing, true);
  pchild.event="paint";
  pchild.pform.signalevent(pchild);
  paintend();
}

// ---------------------------------------------------------------------
void Isigraph2::paintend()
{
  if (painter) {
    if (painter.isActive()) painter.end();
    delete painter;
    painter=0;
  }
}

// ---------------------------------------------------------------------
void Isigraph2::resizeEvent(QResizeEvent *event)
{
  if (type.equals("isigraph")) return;
  QSize s=event.size();
  int w=s.width();
  int h=s.height();
  if (pixmap) {
    if (w > pixmap.width() || h > pixmap.height()) {
      if (painter) delete painter;
      QPixmap *p=new QPixmap(w+128,h+128);
      p.fill(QColor(0,0,0,0));
      painter=new QPainter(p);
      painter.drawPixmap(QPoint(0,0),*pixmap);
      delete pixmap;
      pixmap=p;
    }
  } else {
    pixmap=new QPixmap(w+128,h+128);
    pixmap.fill(QColor(0,0,0,0));
    painter=new QPainter(pixmap);
  }
  if (painter.isActive()) painter.setRenderHint(QPainter::Antialiasing, true);
  pchild.event="resize";
  pchild.pform.signalevent(pchild);
}

// ---------------------------------------------------------------------
void Isigraph2::buttonEvent(QEvent::Type type, QMouseEvent *event)
{
  pchild.pform.isigraph=pchild;

  String lmr = "";
  switch (event.button()) {
  case Qt::LeftButton:
    lmr = "l";
    break;
  case Qt::MidButton:
    lmr = "m";
    break;
  case Qt::RightButton:
    lmr = "r";
    break;
  default:
    break;
  }

  String evtname = "mmove";
  switch (type) {
  case QEvent::MouseButtonPress:
    evtname = "mb" + lmr + "down";
    break;
  case QEvent::MouseButtonRelease:
    evtname = "mb" + lmr + "up";
    break;
  case QEvent::MouseButtonDblClick:
    evtname = "mb" + lmr + "dbl";
    break;
  case QEvent::MouseMove:
    evtname = "mmove";
    break;
  default:
    break;
  }

  // sysmodifiers = shift+2*control
  // sysdata = mousex,mousey,gtkwh,button1,button2,control,shift,button3,0,0,wheel
  char sysmodifiers[20];
  sprintf(sysmodifiers , "%d", (2*(!!(event.modifiers() & Qt::CTRL))) + (!!(event.modifiers() & Qt::SHIFT)));
  char sysdata[200];
  sprintf(sysdata , "%d %d %d %d %d %d %d %d %d %d %d %d", event.x(), event.y(), this.width(), this.height(), (!!(event.buttons() & Qt::LeftButton)), (!!(event.buttons() & Qt::MidButton)), (!!(event.modifiers() & Qt::CTRL)), (!!(event.modifiers() & Qt::SHIFT)), (!!(event.buttons() & Qt::RightButton)), 0, 0, 0);

  pchild.event=evtname;
  pchild.sysmodifiers=String(sysmodifiers);
  pchild.sysdata=String(sysdata);
  pchild.pform.signalevent(pchild);
}

// ---------------------------------------------------------------------
void Isigraph2::wheelEvent(QWheelEvent *event)
{
  pchild.pform.isigraph=pchild;

  char deltasign = ' ';
  int delta = event.delta() / 8;  // degree
  if (delta<0) {
    delta = -delta;
    deltasign = '_';
  }

  // sysmodifiers = shift+2*control
  // sysdata = mousex,mousey,gtkwh,button1,button2,control,shift,button3,0,0,wheel
  char sysmodifiers[20];
  sprintf(sysmodifiers , "%d", (2*(!!(event.modifiers() & Qt::CTRL))) + (!!(event.modifiers() & Qt::SHIFT)));
  char sysdata[200];
  sprintf(sysdata , "%d %d %d %d %d %d %d %d %d %d %d %c%d", event.x(), event.y(), this.width(), this.height(), (!!(event.buttons() & Qt::LeftButton)), (!!(event.buttons() & Qt::MidButton)), (!!(event.modifiers() & Qt::CTRL)), (!!(event.modifiers() & Qt::SHIFT)), (!!(event.buttons() & Qt::RightButton)), 0, 0, deltasign, delta);

  pchild.event=String("mwheel");
  pchild.sysmodifiers=String(sysmodifiers);
  pchild.sysdata=String(sysdata);
  pchild.pform.signalevent(pchild);
}

// ---------------------------------------------------------------------
void Isigraph2::mousePressEvent(QMouseEvent *event)
{
  buttonEvent(QEvent::MouseButtonPress, event);
}

// ---------------------------------------------------------------------
void Isigraph2::mouseMoveEvent(QMouseEvent *event)
{
  buttonEvent(QEvent::MouseMove, event);
}

// ---------------------------------------------------------------------
void Isigraph2::mouseDoubleClickEvent(QMouseEvent *event)
{
  buttonEvent(QEvent::MouseButtonDblClick, event);
}

// ---------------------------------------------------------------------
void Isigraph2::mouseReleaseEvent(QMouseEvent *event)
{
  buttonEvent(QEvent::MouseButtonRelease, event);
}

// ---------------------------------------------------------------------
void Isigraph2::focusInEvent(QFocusEvent *event)
{
  Q_UNUSED(event);
  pchild.event="focus";
  pchild.sysmodifiers="";
  pchild.sysdata="";
  pchild.pform.signalevent(pchild);
}

// ---------------------------------------------------------------------
void Isigraph2::focusOutEvent(QFocusEvent *event)
{
  Q_UNUSED(event);
  pchild.event="focuslost";
  pchild.sysmodifiers="";
  pchild.sysdata="";
  pchild.pform.signalevent(pchild);
}

// ---------------------------------------------------------------------
void Isigraph2::keyPressEvent(QKeyEvent *event)
{
  int key1=0;
  int key=event.key();
  if (ismodifier(key)) return;
#ifdef QT_OS_ANDROID
  if (key==Qt::Key_Back) {
    View::keyPressEvent(event);
    return;
  }
#endif
  if ((key>0x10000ff)||((key>=Qt::Key_F1)&&(key<=Qt::Key_F35))) {
    View::keyPressEvent(event);
    return;
  } else
    key1=translateqkey(key);
  char sysmodifiers[20];
  sprintf(sysmodifiers , "%d", (2*(!!(event.modifiers() & Qt::CTRL))) + (!!(event.modifiers() & Qt::SHIFT)));
  char sysdata[20];
  if (key==key1)
    sprintf(sysdata , "%s", event.text().toUtf8().constData());
  else sprintf(sysdata , "%s", String(QChar(key1)).toUtf8().constData());

  pchild.event=String("char");
  pchild.sysmodifiers=String(sysmodifiers);
  pchild.sysdata=String(sysdata);
  pchild.pform.signalevent(pchild);
  View::keyPressEvent(event);
}
