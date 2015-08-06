#ifndef NMAIN_H
#define NMAIN_H


class Note;
class Ntabs;

class Nmain : public QWidget
{
  Q_OBJECT

public:
  Nmain(Note *n);

  Ntabs *tabBar;
  QToolBar *toolBar;
  QAction *runallAct;

private:

  void createActions();
  void createTabBar();
  void createToolBar();

  QAction *makeact(String id, String icon, String shortcut);

  QAction *lastprojectAct;
  QAction *openprojectAct;
#ifdef QT_OS_ANDROID
  QAction *xeditAct;
  QAction *markCursorAct;
#endif
};

#endif
