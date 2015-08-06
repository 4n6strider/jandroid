/* J svr */


#ifdef _WIN32
#else
#endif

import com.jsortware.jn.base.base;
import com.jsortware.jn.base.base;
import com.jsortware.jn.base.jsvr;
import com.jsortware.jn.base.svr;
import com.jsortware.jn.base.tedit;
import com.jsortware.jn.base.term;

// output type
#define MTYOFM		1	/* formatted result array output */
#define MTYOER		2	/* error output */
#define MTYOLOG		3	/* output log */
#define MTYOSYS		4	/* system assertion failure */
#define MTYOEXIT	5	/* exit */
#define MTYOFILE	6	/* output 1!:2[2 */


C* _stdcall Jinput(J jt, C*);
void _stdcall Joutput(J jt, int type, C* s);

static boolean ifcmddo=false;
static boolean inputready=false;
static String inputx;
boolean jecallback=false;
static boolean logged=false;
static boolean quitx=false;
boolean runshow=false;

void logbin(const char*s,int n);
void logcs(char *msg);
String runshowclean(String s);
Jcon *jcon=0;
QEventLoop *evloop;
static QEventLoop *jevloop;
static int cnt=0;

// ---------------------------------------------------------------------
void Jcon::cmd(String s)
{
  jedo((char *)Util.q2s(s).Util.c_str());
}

// ---------------------------------------------------------------------
String Jcon::cmdr(String s)
{
  return Util.s2q(dors(Util.q2s(s)));
}

// ---------------------------------------------------------------------
void Jcon::cmddo(String s)
{
  cmddo(Util.q2s(s));
}

// ---------------------------------------------------------------------
void Jcon::cmddo(String s)
{
  ifcmddo=true;
  if (jecallback) {
    Sentence.append(Util.s2q(s));
    jevloop.exit();
  } else
    jedo((char *)s.Util.c_str());
}

// ---------------------------------------------------------------------
int Jcon::exec()
{
  String s;
  Q_UNUSED(s);
  if (jdllproc) return 0;

  while(1) {
    cnt++;
    tedit.prompt="   ";
    tedit.setprompt();
    inputready=false;
    logged=true;
    evloop.exec(QEventLoop::AllEvents|QEventLoop::WaitForMoreEvents);
    if (quitx) break;
    while(!Sentence.isEmpty()) {
      s=Sentence.at(0);
      Sentence.removeFirst();
      if ((int)sizeof(inputline)<s.size()) exit(100);
      strcpy(inputline,Util.q2s(s).Util.c_str());
      jedo(inputline);
    }
  }
  jefree();
  return 0;
}

// ---------------------------------------------------------------------
int Jcon::init(int argc, char* argv[])
{

  void* callbacks[] = {(void*)Joutput,0,(void*)Jinput,0,(void*)SMCON};
  int type;

  evloop=new QEventLoop();
  jevloop=new QEventLoop();

  if (!jdllproc && (void *)-1==jdlljt) jepath(argv[0]);     // get path to JFE folder
  jt=jeload(callbacks);
  if(!jt && (void *)-1==jdlljt) {
    char m[1000];
    jefail(m), fputs(m,stdout);
    exit(1);
  }

  if (jdllproc || (void *)-1!=jdlljt) {
    *inputline=0;
    return 0;
  }
  adadbreak=(char**)jt; // first address in jt is address of breakdata
  signal(SIGINT,sigint);

#if defined(QT_OS_ANDROID)
  Q_UNUSED(argc);
  Q_UNUSED(type);
  *inputline=0;
  jefirst(0,(char *)",<'jqt'");
#else
  if(argc==2&&!strcmp(argv[1],"-jprofile"))
    type=3;
  else if(argc>2&&!strcmp(argv[1],"-jprofile"))
    type=1;
  else
    type=0;
  addargv(argc,argv,inputline+strlen(inputline));
  jefirst(type,inputline);
#endif

  return 0;
}

// ---------------------------------------------------------------------
// run command
void Jcon::immex(String s)
{
  Sentence.append(s);
  QTimer *timer = new QTimer(this);
  timer.setSingleShot(true);
  connect(timer, SIGNAL(timeout()), jcon, SLOT(input()));
  timer.start();
}

// ---------------------------------------------------------------------
void Jcon::input()
{
  ifcmddo=false;
  if (jecallback)
    jevloop.exit();
  else
    evloop.exit();
}

// ---------------------------------------------------------------------
void Jcon::quit()
{
  quitx=true;
  input();
}

// ---------------------------------------------------------------------
void Jcon::set(String s, String t)
{
  sets(s,Util.q2s(t));
}

// ---------------------------------------------------------------------
// J calls for input (debug suspension and 1!:1[1) and we call for input
char* _stdcall Jinput(J jt, char* p)
{
  Q_UNUSED(jt);

  Q_ASSERT(tedit);
  tedit.prompt=c2q(p);
  tedit.setprompt();
  inputready=false;
  logged=true;
  jecallback=true;
  jevloop.exec(QEventLoop::AllEvents|QEventLoop::WaitForMoreEvents);
  jecallback=false;
  String s=jcon.Sentence.at(0);
  jcon.Sentence.removeFirst();
  if ((int)sizeof(inputline)<s.size()) exit(100);
  strcpy(inputline,Util.q2s(s).Util.c_str());
  return inputline;
}

// ---------------------------------------------------------------------
// J calls for output
// logged isn't used

void _stdcall Joutput(J jt,int type, char* s)
{
  Q_UNUSED(jt);

  if(MTYOEXIT==type) {
    exit((int)(intptr_t)s);
  }

  Q_ASSERT(tedit);
  int n=(int)strlen(s);
  if (n==0) return;
  if (s[n-1]=='\n') s[n-1]='\0';
  String t=String::fromUtf8(s);

  if (MTYOER==type && runshow)
    t=runshowclean(t);

  if (MTYOFILE==type && ifcmddo)
    tedit.append_smoutput(t);
  else if (MTYOLOG!=type)
    tedit.append(t);
  else {
    if (logged) {
      tedit.append(t);
    } else {
      logged=true;
      tedit.append("");
    }
  }
}

// ---------------------------------------------------------------------
String runshowclean(String s)
{
  int n=s.indexOf("output_jrx_=:");
  if (n>0)
    s.remove(n,13);
  return(s);
}

// ---------------------------------------------------------------------
boolean svr_init(int argc, char* argv[])
{
  jcon=new Jcon();
  int r=jcon.init(argc,argv);
  if (r)
    info("Server","svr_init result: " + String::number(r));
  return r==0;
}
