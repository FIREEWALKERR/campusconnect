import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.zip.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** One-time additive fixture importer; not part of the running application's source. */
public class PopulateDemo {
  static Connection db;
  static Instant now=Instant.now();
  static List<Path> files=new ArrayList<>();
  static Map<String,Integer> counts=new LinkedHashMap<>();
  static final String MARKER="fictional-dataset-v1";
  static long insert(String table,Object... pairs) throws Exception {
    var columns=new ArrayList<String>(List.of("created_at","updated_at","version"));
    var values=new ArrayList<Object>(List.of(Timestamp.from(now),Timestamp.from(now),0L));
    for(int i=0;i<pairs.length;i+=2) { columns.add((String)pairs[i]);values.add(pairs[i+1]); }
    String sql="INSERT INTO "+table+" ("+String.join(",",columns)+") VALUES ("+String.join(",",Collections.nCopies(values.size(),"?"))+")";
    try(var st=db.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
      for(int i=0;i<values.size();i++) st.setObject(i+1,values.get(i)); st.executeUpdate();
      try(var keys=st.getGeneratedKeys()) { keys.next(); counts.merge(table,1,Integer::sum);return keys.getLong(1); }
    }
  }
  static void execute(String sql,Object... values) throws Exception { try(var st=db.prepareStatement(sql)) {for(int i=0;i<values.length;i++)st.setObject(i+1,values[i]);st.executeUpdate();} }
  static Long find(String sql,Object... values) throws Exception {try(var st=db.prepareStatement(sql)){for(int i=0;i<values.length;i++)st.setObject(i+1,values[i]);try(var rs=st.executeQuery()){return rs.next()?rs.getLong(1):null;}}}
  static Timestamp at(int days) {return Timestamp.from(now.plus(Duration.ofDays(days)));}
  static long user(String email,String name,String role,String hash) throws Exception {
    if(find("SELECT id FROM app_user WHERE email=?",email)!=null)throw new IllegalStateException("Sample identity already exists; refusing to overwrite it: "+email);
    return insert("app_user","email",email,"full_name",name,"role",role,"password",hash,"enabled",true);
  }
  static String xml(String value){return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");}
  static String resume(Path directory,String name,String department,int batch,String skills) throws Exception {
    String key=UUID.randomUUID()+".docx";Path file=directory.resolve(key);
    String[] paragraphs={name+" | Fictional demonstration resume",department+" | Graduating batch "+batch,"This document contains synthetic sample data only.","Skills: "+skills,"Education: B.Tech, Demo Engineering College","Project: Campus services platform using Java, SQL and automated tests.","Experience: Academic team project and a simulated internship.","Achievements: Student project showcase finalist."};
    StringBuilder body=new StringBuilder(); for(String p:paragraphs)body.append("<w:p><w:r><w:t>").append(xml(p)).append("</w:t></w:r></w:p>");
    Map<String,String> parts=new LinkedHashMap<>();
    parts.put("[Content_Types].xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>");
    parts.put("_rels/.rels","<?xml version=\"1.0\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/></Relationships>");
    parts.put("word/document.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>"+body+"<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/><w:pgMar w:top=\"1440\" w:right=\"1440\" w:bottom=\"1440\" w:left=\"1440\"/></w:sectPr></w:body></w:document>");
    try(var zip=new ZipOutputStream(Files.newOutputStream(file,StandardOpenOption.CREATE_NEW))){for(var part:parts.entrySet()){zip.putNextEntry(new ZipEntry(part.getKey()));zip.write(part.getValue().getBytes(StandardCharsets.UTF_8));zip.closeEntry();}}
    files.add(file);return key;
  }
  record Student(long id,long user,long dept,int year,BigDecimal cgpa,int backlogs,String name,String email){}
  record Job(long id,long recruiter,int number,BigDecimal minimum,int backlogs,Set<Long> departments,Set<Integer> batches,String status){}
  public static void main(String[] args) throws Exception {
    Path project=Path.of(args[0]);Map<String,String> env=new HashMap<>();
    for(String line:Files.readAllLines(project.resolve(".env"))){if(!line.isBlank()&&!line.startsWith("#")){String[] pair=line.split("=",2);env.put(pair[0],pair.length==2?pair[1]:"");}}
    String password="Demo!"+HexFormat.of().formatHex(java.security.SecureRandom.getSeed(8));
    String hash=new BCryptPasswordEncoder(12).encode(password);
    Path resumeDir=Path.of(env.getOrDefault("RESUME_DIRECTORY","./data/resumes")); if(!resumeDir.isAbsolute()) resumeDir=project.resolve(resumeDir); Files.createDirectories(resumeDir);
    Path guide=project.resolve("demo-data/GENERATED-ACCOUNTS.md");
    try(var connection=DriverManager.getConnection(env.get("DB_URL"),env.get("DB_USERNAME"),env.get("DB_PASSWORD"))){
      db=connection;db.setAutoCommit(false);
      try{
        if(find("SELECT id FROM audit_log WHERE target=?",MARKER)!=null){System.out.println("Dataset already imported; no changes made.");return;}
        Long admin=find("SELECT id FROM app_user WHERE role='ADMIN' ORDER BY id LIMIT 1");if(admin==null)throw new IllegalStateException("An administrator is required.");
        int year=LocalDate.now(ZoneId.of(env.getOrDefault("APP_TIME_ZONE","Asia/Kolkata"))).getYear();
        String[] codes={"CSE","IT","ECE","EEE","MECHANICAL","CIVIL"};
        String[] departmentNames={"Computer Science and Engineering","Information Technology","Electronics and Communication","Electrical and Electronics","Mechanical Engineering","Civil Engineering"};
        long[] departments=new long[codes.length];
        for(int i=0;i<codes.length;i++){Long id=find("SELECT id FROM department WHERE code=?",codes[i]);departments[i]=id==null?insert("department","code",codes[i],"name",departmentNames[i]):id;}
        String[] skillNames={"java","sql","spring boot","git","html","css","data analysis","software testing","communication","problem solving","embedded systems","cad"};long[] skills=new long[skillNames.length];
        for(int i=0;i<skills.length;i++){Long id=find("SELECT id FROM skill WHERE name=?",skillNames[i]);skills[i]=id==null?insert("skill","name",skillNames[i]):id;}
        String[] first={"Aarav","Ananya","Ishaan","Diya","Kabir","Sara","Rohan","Mira","Arjun","Kavya","Aditya","Nisha","Vihaan","Ira","Dev","Tara","Neel","Sana"};
        String[] last={"Mehta","Rao","Patel","Nair","Singh","Khan","Das","Iyer","Shah","Verma","Joshi","Menon"};
        var students=new ArrayList<Student>();var logins=new StringBuilder("| Role | Name | Email |\n|---|---|---|\n");
        for(int n=0;n<36;n++){
          String name=first[n%first.length]+" "+last[(n*5+n/first.length)%last.length];String email=String.format("student%02d@demo.campusconnect.test",n+1);
          long uid=user(email,name,"STUDENT",hash);int batch=year+1+n%3;BigDecimal cgpa=BigDecimal.valueOf(620+(n*17)%340,2);int backlogs=n%3;
          String key=resume(resumeDir,name,departmentNames[n%6],batch,"Java, SQL, Git, communication, problem solving");
          long sid=insert("student_profile","user_id",uid,"registration_number",String.format("DEMO-%d-%03d",year,n+1),"phone","0000000000","department_id",departments[n%6],"degree","B.Tech","graduation_year",batch,"cgpa",cgpa,"backlogs",backlogs,"academic_history","Fictional record: B.Tech at Demo Engineering College. Class XII: "+(75+n%20)+"%; Class X: "+(80+n%16)+"%.","certifications","Java foundations; database design; workplace communication (sample certifications).","projects","CampusConnect practice project; department inventory tracker; collaborative engineering capstone.","achievements","Fictional achievements: project showcase finalist; coding club volunteer.","resume_key",key);
          for(int k=0;k<5;k++)execute("INSERT INTO student_skill(student_id,skill_id) VALUES (?,?)",sid,skills[(n+k)%skills.length]);
          students.add(new Student(sid,uid,departments[n%6],batch,cgpa,backlogs,name,email));logins.append("| Student | ").append(name).append(" | ").append(email).append(" |\n");
          insert("notification","user_id",uid,"message","Your fictional demonstration profile and sample resume are ready.","read_flag",n%3==0);
        }
        long[] coordinators=new long[2];for(int n=0;n<2;n++){String email="coordinator"+(n+1)+"@demo.campusconnect.test";String name=n==0?"Priya Sen":"Vikram Bose";coordinators[n]=user(email,name,"COORDINATOR",hash);logins.append("| Coordinator | ").append(name).append(" | ").append(email).append(" |\n");}
        String[] companyNames={"Nexora Labs (Demo)","BluePeak Systems (Demo)","Vertex Engineering (Demo)","Orbit Analytics (Demo)","CedarCloud Solutions (Demo)","Aster Robotics (Demo)","Lumen Digital (Demo)","Summit Infra (Demo)"};
        String[] cities={"Bengaluru","Pune","Hyderabad","Chennai","Remote","Mumbai","Noida","Ahmedabad"};
        long[] companies=new long[8],recruiters=new long[8];
        for(int n=0;n<8;n++){String email=String.format("recruiter%02d@demo.campusconnect.test",n+1);recruiters[n]=user(email,companyNames[n]+" Recruitment","COMPANY",hash);companies[n]=insert("company","user_id",recruiters[n],"name",companyNames[n],"website","https://example.com","location",cities[n],"description","Fictional company for campus placement demonstrations. Not a real employer. Graduate roles include mentoring, structured onboarding and collaborative engineering projects.","approval",n<6?"APPROVED":n==6?"PENDING":"REJECTED");logins.append("| Recruiter | ").append(companyNames[n]).append(" | ").append(email).append(" |\n");}
        long[] drives=new long[3];
        for(int n=0;n<3;n++)drives[n]=insert("placement_drive","name",new String[]{"Graduate Recruitment Week (Demo)","Industry Internship Fair (Demo)","Engineering Careers Drive (Demo)"}[n],"academic_year",(year+n)+"-"+(year+n+1),"batch",year+n+1,"start_date",java.sql.Date.valueOf(LocalDate.now().minusDays(21)),"end_date",java.sql.Date.valueOf(LocalDate.now().plusDays(45+n*20)),"venue","Campus Career Centre - Demo Hall "+(n+1));
        String[] titles={"Associate Software Engineer","Graduate Engineer Trainee","Data Analyst Intern","Java Backend Developer","Quality Assurance Engineer","Cloud Support Associate","Embedded Systems Engineer","Design Engineer"};var jobs=new ArrayList<Job>();
        for(int n=0;n<24;n++){
          int c=n/3;String status=c>=6?"DRAFT":n%3==2?"CLOSED":"PUBLISHED";BigDecimal minimum=BigDecimal.valueOf(600+(n%4)*50,2);int maxBacklogs=n%3;Set<Long> eligible=new LinkedHashSet<>();for(int d=0;d<6;d++)if(n%3==0||d%2==n%2)eligible.add(departments[d]);Set<Integer> batches=Set.of(year+1,year+2,year+3);
          long id=insert("job","company_id",companies[c],"drive_id",drives[n%3],"title",titles[n%titles.length],"description","Fictional placement opening for demonstration. Work with a graduate team to build, test and document engineering solutions. Selection includes profile review, technical discussion and HR interview. No real employment commitment is created by this sample record.","required_skills",n%2==0?"Java, SQL, Git, problem solving, communication":"Engineering fundamentals, testing, data analysis, teamwork","category",n%2==0?"Software Engineering":"Core Engineering","location",cities[c],"employment_type",n%4==2?"INTERNSHIP":"FULL_TIME","salary_min",BigDecimal.valueOf(400000+(n%8)*100000),"salary_max",BigDecimal.valueOf(650000+(n%8)*125000),"minimum_cgpa",minimum,"maximum_backlogs",maxBacklogs,"opens_at",at(-25),"deadline",status.equals("CLOSED")?at(-1):at(10+n),"vacancies",5+n%10,"status",status);
          for(long d:eligible)execute("INSERT INTO job_department(job_id,department_id) VALUES (?,?)",id,d);for(int b:batches)execute("INSERT INTO job_batch(job_id,batches) VALUES (?,?)",id,b);
          jobs.add(new Job(id,recruiters[c],n,minimum,maxBacklogs,eligible,batches,status));
        }
        String[] targets={"APPLIED","UNDER_REVIEW","SHORTLISTED","INTERVIEW_SCHEDULED","SELECTED","OFFERED","OFFER_ACCEPTED","REJECTED","WITHDRAWN","OFFER_DECLINED"};
        var normal=List.of("APPLIED","UNDER_REVIEW","SHORTLISTED","INTERVIEW_SCHEDULED","SELECTED","OFFERED","OFFER_ACCEPTED");int counter=0;
        for(var s:students){int taken=0;for(var j:jobs){if(j.status().equals("DRAFT")||s.cgpa().compareTo(j.minimum())<0||s.backlogs()>j.backlogs()||!j.departments().contains(s.dept()))continue;
          String target=targets[counter++%targets.length];List<String> path;
          if(target.equals("REJECTED"))path=List.of("APPLIED","UNDER_REVIEW","REJECTED");else if(target.equals("WITHDRAWN"))path=List.of("APPLIED","UNDER_REVIEW","WITHDRAWN");else if(target.equals("OFFER_DECLINED"))path=List.of("APPLIED","UNDER_REVIEW","SHORTLISTED","INTERVIEW_SCHEDULED","SELECTED","OFFERED","OFFER_DECLINED");else path=normal.subList(0,normal.indexOf(target)+1);
          long application=insert("job_application","student_id",s.id(),"job_id",j.id(),"status",target);execute("UPDATE job_application SET created_at=?,updated_at=? WHERE id=?",at(-14),at(-1),application);
          String previous=null;for(int step=0;step<path.size();step++){String state=path.get(step);long actor=state.equals("APPLIED")||state.equals("WITHDRAWN")||state.startsWith("OFFER_A")||state.equals("OFFER_DECLINED")?s.user():j.recruiter();long history=insert("application_status_history","application_id",application,"actor_id",actor,"previous_status",previous,"status",state,"note","Fictional demonstration recruitment event: "+state);execute("UPDATE application_status_history SET created_at=?,updated_at=? WHERE id=?",at(-14+step*2),at(-14+step*2),history);previous=state;}
          if(path.contains("INTERVIEW_SCHEDULED")){boolean upcoming=target.equals("INTERVIEW_SCHEDULED");insert("interview","application_id",application,"round_number",1,"type","Technical discussion","location",upcoming?"Demo Career Centre, Room "+(100+counter):"Demo online interview room","instructions","Sample interview: bring the fictional resume and prepare to discuss academic projects.","scheduled_at",upcoming?at(2+counter%5):at(-7),"status",upcoming?"SCHEDULED":"COMPLETED");insert("notification","user_id",s.user(),"message","Interview details for application #"+application+" are available in your schedule.","read_flag",!upcoming);}
          if(path.contains("OFFERED")){String state=target.equals("OFFER_ACCEPTED")?"ACCEPTED":target.equals("OFFER_DECLINED")?"DECLINED":"PENDING";insert("placement_offer","application_id",application,"salary",BigDecimal.valueOf(650000+(counter%9)*125000),"offer_date",at(-4),"acceptance_deadline",at(7+counter%5),"responded_at",state.equals("PENDING")?null:at(-2),"status",state);insert("notification","user_id",s.user(),"message","A fictional placement offer is available for application #"+application+".","read_flag",!state.equals("PENDING"));}
          insert("notification","user_id",s.user(),"message","Application #"+application+" status: "+target,"read_flag",counter%2==0);
          if(++taken==3)break;
        }}
        String[] news={"Placement registration is open","Resume review workshop","Technical interview preparation","Internship fair schedule","Offer response deadline reminder","Next month's career readiness session"};
        for(int n=0;n<news.length;n++){insert("announcement","author_id",coordinators[n%2],"title",news[n]+" (Demo)","body","Fictional campus announcement for demonstration. Complete your profile, review the listed opportunities and follow the interview instructions. Dates and outcomes shown in this sample dataset do not refer to real recruitment.","published_at",n==5?at(5):at(-n),"notified_at",n==5?null:at(0));}
        for(var s:students)insert("notification","user_id",s.user(),"message","New campus announcements and placement deadlines are available.","read_flag",false);
        insert("audit_log","actor_id",admin,"action","Imported fictional demonstration dataset; existing records preserved","target",MARKER);
        String guideText="# Dummy dataset and logins\n\nAll records below are fictional. Existing administrator and student records were preserved.\n\nShared password for these newly created dummy accounts: `"+password+"`\n\nAdministrator: continue using your existing account and password.\n\n"+logins+"\nImported records:\n\n"+counts+"\n\nThe dataset includes complete academic profiles, skills, synthetic resumes, company approval states, job eligibility rules, multiple batches, drives, application histories, interviews, offers, notifications and announcements. Dates are relative to the import date.\n";
        Files.writeString(guide,guideText,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
        db.commit();System.out.println("Import committed: "+counts);System.out.println("New fictional applications: "+counter);System.out.println("Login guide: "+guide);
      }catch(Exception e){db.rollback();for(Path file:files)Files.deleteIfExists(file);throw e;}
    }
  }
}

