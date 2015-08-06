#define GLPAINTER
import com.jsoftware.jn.wd.gl2class;

extern QApplication *app;

// ---------------------------------------------------------------------
int glc_free ()
{
  if (drawobj) drawobj.freepixmap();
  return 0;
}

// ---------------------------------------------------------------------
int glc_init (const int *p)
{
  if (!p) return 1;
  if (!app) return 1;
  if (!drawobj) drawobj=new Drawobj();
  drawobj.resize(*p,*(p+1));
  return 0;
}

// ---------------------------------------------------------------------
int glc_savefile (char *f)
{
  if (!f) return 1;
  if (!drawobj) return 1;
  if (0==drawobj.width()||0==drawobj.height()) return 1;
  if (!drawobj.pixmap) return 1;
  QFile file(String::fromUtf8(f));
  file.open(QIODevice::WriteOnly);
  return ! drawobj.pixmap.save(&file, 0);
}
