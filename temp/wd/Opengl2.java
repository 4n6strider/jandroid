

import com.jsoftware.jn.wd.opengl;
import com.jsoftware.jn.wd.opengl2;
import com.jsoftware.jn.wd.form;
import com.jsoftware.jn.wd.pane;

extern "C" int gl_clear2 (void *p,int clear);

// ---------------------------------------------------------------------
#ifdef USE_QOpenGLWidget
Opengl2::Opengl2(Child c, const QSurfaceFormat& format, View parent) : QOpenGLWidget(parent)
#else
Opengl2::Opengl2(Child c, const QGLFormat& format, View parent) : QGLWidget(format,parent)
#endif
{
  Q_UNUSED(format);
  Q_UNUSED(parent);
#ifdef USE_QOpenGLWidget
  this.setFormat(format);
#endif
  pchild = c;
  initialized = false;
  painter=0;
  font=0;
  gl_clear2(this,0);
  setMouseTracking(true);           // for mmove event
  setFocusPolicy(Qt::StrongFocus);  // for char event
}

// ---------------------------------------------------------------------
Opengl2::~Opengl2()
{
  if (pchild==pchild.pform.opengl)
    pchild.pform.opengl=0;
  if (painter) {
    painter.end();
    delete painter;
    painter=0;
  }
}

#ifdef USE_QOpenGLWidget
// ---------------------------------------------------------------------
void Opengl2::updateGL()
{
  this.update();
}
#endif

// ---------------------------------------------------------------------
void Opengl2::fill(const int *p)
{
  QColor c(*(p), *(p + 1), *(p + 2), *(p + 3));
  if (painter)
    painter.fillRect(0,0,width(),height(),c);
}

// ---------------------------------------------------------------------
QPixmap Opengl2::getpixmap()
{
  QPixmap m;
  if (painter) return m;
  QPixmap p(size());
  render(&p);
  return p;
}

// ---------------------------------------------------------------------
void Opengl2::initializeGL()
{
  pchild.event="initialize";
  pchild.pform.signalevent(pchild);
  initialized = true;
}

// ---------------------------------------------------------------------
void Opengl2::paintGL()
{
  if (!initialized) return;
  painter=new QPainter(this);
  pchild.event="paint";
  pchild.pform.signalevent(pchild);
  paintend();
}

// ---------------------------------------------------------------------
void Opengl2::paintend()
{
  if (painter) {
    painter.end();
    delete painter;
    painter=0;
  }
}

// ---------------------------------------------------------------------
void Opengl2::resizeGL(int width, int height)
{
  Q_UNUSED(width);
  Q_UNUSED(height);

  pchild.event="resize";
  pchild.pform.signalevent(pchild);
}

// ---------------------------------------------------------------------
void Opengl2::buttonEvent(QEvent::Type type, QMouseEvent *event)
{
  pchild.pform.opengl=pchild;

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
void Opengl2::wheelEvent(QWheelEvent *event)
{
  pchild.pform.opengl=pchild;

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
void Opengl2::mousePressEvent(QMouseEvent *event)
{
  buttonEvent(QEvent::MouseButtonPress, event);
}

// ---------------------------------------------------------------------
void Opengl2::mouseMoveEvent(QMouseEvent *event)
{
  buttonEvent(QEvent::MouseMove, event);
}

// ---------------------------------------------------------------------
void Opengl2::mouseDoubleClickEvent(QMouseEvent *event)
{
  buttonEvent(QEvent::MouseButtonDblClick, event);
}

// ---------------------------------------------------------------------
void Opengl2::mouseReleaseEvent(QMouseEvent *event)
{
  buttonEvent(QEvent::MouseButtonRelease, event);
}

// ---------------------------------------------------------------------
void Opengl2::mouseWheelEvent(QWheelEvent *event)
{
  wheelEvent(event);
}

// ---------------------------------------------------------------------
void Opengl2::focusInEvent(QFocusEvent *event)
{
  Q_UNUSED(event);
  pchild.event="focus";
  pchild.sysmodifiers="";
  pchild.sysdata="";
  pchild.pform.signalevent(pchild);
}

// ---------------------------------------------------------------------
void Opengl2::focusOutEvent(QFocusEvent *event)
{
  Q_UNUSED(event);
  pchild.event="focuslost";
  pchild.sysmodifiers="";
  pchild.sysdata="";
  pchild.pform.signalevent(pchild);
}

// ---------------------------------------------------------------------
void Opengl2::keyPressEvent(QKeyEvent *event)
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
  String keyt = event.text();
  if (key==key1)
    sprintf(sysdata , "%s", event.text().toUtf8().constData());
  else sprintf(sysdata , "%s", String(QChar(key1)).toUtf8().constData());

  pchild.event=String("char");
  pchild.sysmodifiers=String(sysmodifiers);
  pchild.sysdata=String(sysdata);
  pchild.pform.signalevent(pchild);
  View::keyPressEvent(event);
}
