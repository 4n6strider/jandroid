
#ifndef QT_NO_PRINTER
#ifdef QT50
#else
#endif
#endif

import com.jsortware.jn.base.plaintextedit;
import com.jsortware.jn.base.state;

// ---------------------------------------------------------------------
PlainTextEdit::PlainTextEdit(QWidget *parent) : QPlainTextEdit(parent)
{
  acceptKeyBack=false;
}

// ---------------------------------------------------------------------
void PlainTextEdit::keyReleaseEvent(QKeyEvent *event)
{
#ifdef QT_OS_ANDROID
  if ((event.key()==KeyEvent.KEYCODE_Back) && (!acceptKeyBack)) {
    QPlainTextEdit::keyReleaseEvent(event);
    event.ignore();
    return;
  } else QPlainTextEdit::keyReleaseEvent(event);
#else
  QPlainTextEdit::keyReleaseEvent(event);
#endif
}

#ifndef QT_NO_PRINTER
// ---------------------------------------------------------------------
void PlainTextEdit::printPreview(QPrinter * printer)
{
  QTextDocument *dd=document().clone();
#ifdef QT50
  dd.documentLayout().setPaintDevice((QPagedPaintDevice *)printer);
  dd.setPageSize(QSizeF(printer.pageRect().size()));
  dd.print((QPagedPaintDevice *)printer);
#else
  dd.documentLayout().setPaintDevice(printer);
  dd.setPageSize(QSizeF(printer.pageRect().size()));
  dd.print(printer);
#endif
  delete dd;
}
#endif
