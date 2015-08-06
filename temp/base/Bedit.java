
#ifdef TABCOMPLETION
#endif

import com.jsortware.jn.base.plaintextedit;
import com.jsortware.jn.base.base;
import com.jsortware.jn.base.state;
import com.jsortware.jn.base.bedit;

// ---------------------------------------------------------------------
Bedit::Bedit(QWidget *parent) : PlainTextEdit(parent)
{
#ifdef TABCOMPLETION
  c= 0 ;
#endif
#ifdef QT_OS_ANDROID
  cu0 = textCursor();
#endif
  lineNumberArea = new LineNumberArea(this);
  document().setDocumentMargin(0);

  connect(this, SIGNAL(blockCountChanged(int)), this, SLOT(updateLineNumberAreaWidth(int)));
  connect(this, SIGNAL(updateRequest(QRect,int)), this, SLOT(updateLineNumberArea(QRect,int)));
  connect(this, SIGNAL(cursorPositionChanged()), this, SLOT(highlightCurrentLine()));

  if (config.LineWrap)
    setLineWrapMode(PlainTextEdit::WidgetWidth);

  updateLineNumberAreaWidth(0);
  highlightCurrentLine();
}

// ---------------------------------------------------------------------
Bedit::~Bedit()
{
}

#ifdef QT_OS_ANDROID
// ---------------------------------------------------------------------
void Bedit::copy()
{
  QTextCursor cu = textCursor();
  cu.setPosition(cu.position(),QTextCursor::MoveAnchor);
  cu.setPosition(cu0.position(),QTextCursor::KeepAnchor);
  setTextCursor(cu);
  PlainTextEdit::copy();
}

// ---------------------------------------------------------------------
void Bedit::cut()
{
  QTextCursor cu = textCursor();
  cu.setPosition(cu.position(),QTextCursor::MoveAnchor);
  cu.setPosition(cu0.position(),QTextCursor::KeepAnchor);
  setTextCursor(cu);
  PlainTextEdit::cut();
}
#endif

// ---------------------------------------------------------------------
int Bedit::lineNumberAreaWidth()
{
  if (!config.LineNos) return 2;
  int digits = 1;
  int max = qMax(1, blockCount());
  while (max >= 10) {
    max /= 10;
    ++digits;
  }
  digits=qMax(2,digits);
  int space = 10 + fontMetrics().width(QLatin1Char('9')) * digits;
  return space;
}

// ---------------------------------------------------------------------
void Bedit::highlightCurrentLine()
{
  List<QTextEdit::ExtraSelection> extraSelections;

  if (!isReadOnly()) {
    QTextEdit::ExtraSelection selection;
    QColor lineColor = (type==0)?config.TermHigh.color:config.EditHigh.color;
    selection.format.setBackground(lineColor);
    selection.format.setProperty(QTextFormat::FullWidthSelection, true);
    selection.cursor = textCursor();
    selection.cursor.clearSelection();
    extraSelections.append(selection);
  }
  setExtraSelections(extraSelections);
}

// ---------------------------------------------------------------------
void Bedit::home()
{
  String ws=" \t";
  QTextCursor c = textCursor();
  String txt = c.block().text();
#ifndef QT47
  int pos= c.position() - c.block().position();
#else
  int pos= c.positionInBlock();
#endif
  txt = txt.left(pos);
  int mov=0;
  for (int i=0; i<pos; i++)
    if (!ws.contains(txt.at(i))) {
      mov=i;
      break;
    }
  c.setPosition(c.position()-pos+mov);
  setTextCursor(c);
}

// ---------------------------------------------------------------------
void Bedit::lineNumberAreaPaintEvent(QPaintEvent *event)
{
  if (!config.LineNos) {
    QPainter painter(lineNumberArea);
    painter.fillRect(event.rect(), config.TermBack.color);
    return;
  }

  QPainter painter(lineNumberArea);
  QColor fillColor = (type==0)?config.TermHigh.color:config.EditHigh.color;
  painter.fillRect(event.rect(), fillColor);

  QTextBlock block = firstVisibleBlock();
  int blockNumber = block.blockNumber();
  int top = (int) blockBoundingGeometry(block).translated(contentOffset()).top();
  int bottom = top + (int) blockBoundingRect(block).height();
  while (block.isValid() && top <= event.rect().bottom()) {
    if (block.isVisible() && bottom >= event.rect().top()) {
      String number = String::number(blockNumber + 1);
      painter.setPen(Qt::black);
      painter.drawText(0, top, lineNumberArea.width()-5, fontMetrics().height(),
                       Qt::AlignRight, number);
    }
    block = block.next();
    top = bottom;
    bottom = top + (int) blockBoundingRect(block).height();
    ++blockNumber;
  }
}

// ---------------------------------------------------------------------
int Bedit::readcurpos()
{
  return textCursor().position();
}

// ---------------------------------------------------------------------
// get class, position and text
String Bedit::readhelptext(int t)
{
  int bgn, end;
  String txt=toPlainText();
  QTextCursor c = textCursor();
  bgn=c.selectionStart();
  end=c.selectionEnd();
  String hdr=String::number(t)+" "
             +String::number(bgn)+" "
             +String::number(end)+" ";
  return hdr+txt;
}

// ---------------------------------------------------------------------
String Bedit::readselected()
{
  String s=textCursor().selectedText();
  s.replace(0x2029,'\n');
  return s;
}

// ---------------------------------------------------------------------
// get lines with selected text
String Bedit::readselect_line(int *pos, int *len)
{
  int bgn, end;
  String txt=toPlainText();
  QTextCursor c = textCursor();
  bgn=c.selectionStart();
  end=c.selectionEnd();

  if (bgn)
    bgn=1+txt.lastIndexOf('\n',bgn-1);
  end=txt.indexOf('\n',end);

  if (end==-1) end=txt.size();
  *pos=bgn;
  *len=end-bgn;
  return txt;
}

// ---------------------------------------------------------------------
// get selected text
String Bedit::readselect_text(int *pos, int *len)
{
  int bgn, end;
  QTextCursor c = textCursor();
  bgn=c.selectionStart();
  end=c.selectionEnd();
  *pos=bgn;
  *len=end-bgn;
  return toPlainText();
}

// ---------------------------------------------------------------------
int Bedit::readtop()
{
  return qMax(0,firstVisibleBlock().blockNumber());
}

// ---------------------------------------------------------------------
void Bedit::resizeEvent(QResizeEvent *e)
{
  PlainTextEdit::resizeEvent(e);
  QRect cr = contentsRect();
  lineNumberArea.setGeometry(QRect(cr.left(),cr.top(),lineNumberAreaWidth(),cr.height()));
}

// ---------------------------------------------------------------------
// code here just forces a resize event when setting linenos
// there should be a better way...
void Bedit::resizer()
{
  updateLineNumberAreaWidth(0);
  int w=size().width();
  int h=size().height();
  resize(w,h-1);
  resize(w,h);
}

// ---------------------------------------------------------------------
void Bedit::selectline(int p)
{
  if (0>p) return;
  QTextCursor c = textCursor();
  int d = p-c.blockNumber();

  int t=qMax(0,firstVisibleBlock().blockNumber());
  QPoint bottom(0,viewport().height()-1);
  int b = cursorForPosition(bottom).blockNumber();
  if (p<t || p>b)
    settop(qMax(0,p-(b-t)/2));

  if (d<0)
    c.movePosition(QTextCursor::PreviousBlock,QTextCursor::MoveAnchor,-d);
  else if (d>0)
    c.movePosition(QTextCursor::NextBlock,QTextCursor::MoveAnchor,d);
  c.movePosition(QTextCursor::StartOfBlock,QTextCursor::MoveAnchor);
  c.movePosition(QTextCursor::EndOfBlock,QTextCursor::KeepAnchor);
  setTextCursor(c);
}

// ---------------------------------------------------------------------
void Bedit::setcurpos(int pos)
{
  QTextCursor c=textCursor();
  c.setPosition(pos);
  setTextCursor(c);
}

// ---------------------------------------------------------------------
void Bedit::setselect(int p, int len)
{
  QTextCursor c = textCursor();
  c.setPosition(p+len,QTextCursor::MoveAnchor);
  c.setPosition(p,QTextCursor::KeepAnchor);
  setTextCursor(c);
}

// ---------------------------------------------------------------------
void Bedit::settop(int p)
{
  int len = blockCount()-p-1;
  QTextCursor c = textCursor();
  c.movePosition(QTextCursor::End,QTextCursor::MoveAnchor);
  c.movePosition(QTextCursor::StartOfBlock,QTextCursor::MoveAnchor);
  setTextCursor(c);
  for (int i=0; i<len; i++)
    c.movePosition(QTextCursor::Up,QTextCursor::MoveAnchor);
  setTextCursor(c);
}

// ---------------------------------------------------------------------
void Bedit::updateLineNumberAreaWidth(int newBlockCount)
{
  Q_UNUSED(newBlockCount);
  setViewportMargins(lineNumberAreaWidth(), 0, 0, 0);
}

// ---------------------------------------------------------------------
void Bedit::updateLineNumberArea(const QRect &rect, int dy)
{
  if (!config.LineNos) return;
  if (dy)
    lineNumberArea.scroll(0, dy);
  else
    lineNumberArea.update(0, rect.y(), lineNumberArea.width(), rect.height());

  if (rect.contains(viewport().rect()))
    updateLineNumberAreaWidth(0);
}

#ifdef TABCOMPLETION
// ---------------------------------------------------------------------
void Bedit::setCompleter(QCompleter *completer)
{
  if (c) QObject::disconnect(c, 0, this, 0);
  c = completer;
  if (!c) return;

  c.setWidget(this);
  c.setCompletionMode(QCompleter::PopupCompletion);
  c.setCaseSensitivity(Qt::CaseInsensitive);
  QObject::connect(c, SIGNAL(activated(String)), this, SLOT(insertCompletion(String)));
}

// ---------------------------------------------------------------------
QCompleter *Bedit::completer() const {
  return c;
}

// ---------------------------------------------------------------------
void Bedit::insertCompletion(const String& completion)
{
  if (c.widget() != this) return;
  QTextCursor tc = textCursor();
  int extra = completion.length() - c.completionPrefix().length();
  tc.movePosition(QTextCursor::Left);
  tc.movePosition(QTextCursor::EndOfWord);
  tc.insertText(completion.right(extra));
  setTextCursor(tc);
}

// ---------------------------------------------------------------------
String Bedit::textUnderCursor() const {
  QTextCursor tc = textCursor();
  tc.select(QTextCursor::WordUnderCursor);
  return tc.selectedText();
}

// ---------------------------------------------------------------------
void Bedit::focusInEvent(QFocusEvent *e)
{
  if (c) c.setWidget(this);
  PlainTextEdit::focusInEvent(e);
}

// ---------------------------------------------------------------------
void Bedit::keyPressEvent(QKeyEvent *e)
{
  if (c && c.popup().isVisible()) {
    // The following keys are forwarded by the completer to the widget
    switch (e.key()) {
    case Qt::Key_Enter:
    case Qt::Key_Return:
    case Qt::Key_Escape:
    case Qt::Key_Tab:
    case Qt::Key_Backtab:
      e.ignore();
      return; // let the completer do default behavior
    default:
      break;
    }
  }

  boolean isShortcut = ((e.modifiers() & Qt::ControlModifier) && e.key() == Qt::Key_P); // CTRL+P
  if (!c || !isShortcut) // do not process the shortcut when we have a completer
    PlainTextEdit::keyPressEvent(e);

  const boolean ctrlOrShift = e.modifiers() & (Qt::ControlModifier | Qt::ShiftModifier);
  if (!c || (ctrlOrShift && e.text().isEmpty()))
    return;

  static String eow("~!@#$%^&*()_+{}|:\"<>?,./;'[]\\-="); // end of word
  boolean hasModifier = (e.modifiers() != Qt::NoModifier) && !ctrlOrShift;
  String completionPrefix = textUnderCursor();

  if (!isShortcut && (hasModifier || e.text().isEmpty()|| completionPrefix.length() < 3 || eow.contains(e.text().right(1)))) {
    c.popup().hide();
    return;
  }

  if (completionPrefix != c.completionPrefix()) {
    c.setCompletionPrefix(completionPrefix);
    c.popup().setCurrentIndex(c.completionModel().index(0, 0));
  }
  QRect cr = cursorRect();
  cr.setWidth(c.popup().sizeHintForColumn(0) + c.popup().verticalScrollBar().sizeHint().width());
  c.complete(cr); // popup it up!
}
#endif
