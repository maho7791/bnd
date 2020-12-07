package test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;

import aQute.bnd.build.Project;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.ExportedPackage;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.properties.Document;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import junit.framework.TestCase;

public class BndEditModelTest extends TestCase {
	static CapReqBuilder cp = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE);

	public static void testVariableInRunRequirements() throws Exception {
		Workspace ws = new Workspace(new File("testresources/ws"));
		BndEditModel model = new BndEditModel(ws);
		File f = new File("testresources/ws/p7/reuse.bndrun");
		model.setBndResource(f);
		model.setBndResourceName("reuse.bndrun");
		model.loadFrom(f);

		// VERIFY
		Processor processor = model.getProperties();
		String runrequirements = processor.mergeProperties(Constants.RUNREQUIRES);
		String[] rrr = runrequirements.split(",");
		assertEquals(4, rrr.length);
		assertEquals("osgi.identity;filter:='(osgi.identity=variable)'", rrr[0]);
		assertEquals("osgi.identity;filter:='(osgi.identity=variable2)'", rrr[1]);
		assertEquals("osgi.identity;filter:='(osgi.identity=b)'", rrr[2]);
		assertEquals("osgi.identity;filter:='(osgi.identity=c)'", rrr[3]);

		// [cs] don't know how to update this.
		List<Requirement> r = model.getRunRequires();
		assertEquals(3, r.size());
		assertEquals(new CapReqBuilder("${var}").buildSyntheticRequirement(), r.get(0));
		assertEquals(new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE)
			.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(osgi.identity=b)")
			.buildSyntheticRequirement(), r.get(1));
		assertEquals(new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE)
			.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(osgi.identity=c)")
			.buildSyntheticRequirement(), r.get(2));

		// Test Set with variables
		List<Requirement> rr = new LinkedList<>();
		rr.add(new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE)
			.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(osgi.identity=b)")
			.buildSyntheticRequirement());
		rr.add(new CapReqBuilder("${var}").buildSyntheticRequirement());
		model.setRunRequires(rr);

		// VERIFY
		processor = model.getProperties();
		runrequirements = processor.mergeProperties(Constants.RUNREQUIRES);
		rrr = runrequirements.split(",");
		assertEquals(3, rrr.length);
		assertEquals("osgi.identity;filter:='(osgi.identity=b)'", rrr[0]);
		assertEquals("osgi.identity;filter:='(osgi.identity=variable)'", rrr[1]);
		assertEquals("osgi.identity;filter:='(osgi.identity=variable2)'", rrr[2]);

		// Test SET
		rr = new LinkedList<>();
		rr.add(getReq("(osgi.identity=b)"));
		rr.add(getReq("(osgi.identity=c)"));
		model.setRunRequires(rr);

		// VERIFY
		processor = model.getProperties();
		runrequirements = processor.mergeProperties(Constants.RUNREQUIRES);
		rrr = runrequirements.split(",");
		assertEquals(2, rrr.length);
		assertEquals("osgi.identity;filter:='(osgi.identity=b)'", rrr[0]);
		assertEquals("osgi.identity;filter:='(osgi.identity=c)'", rrr[1]);

		r = model.getRunRequires();
		assertEquals(getReq("(osgi.identity=b)"), r.get(0));
		assertEquals(getReq("(osgi.identity=c)"), r.get(1));

		// TEST Saving changes and those changes persist...
		Document d = new Document("");
		model.saveChangesTo(d);

		model.loadFrom(d); // reset the properties

		runrequirements = processor.mergeProperties(Constants.RUNREQUIRES);
		rrr = runrequirements.split(",");
		assertEquals(2, rrr.length);
		assertEquals("osgi.identity;filter:='(osgi.identity=b)'", Strings.trim(rrr[0]));
		assertEquals("osgi.identity;filter:='(osgi.identity=c)'", Strings.trim(rrr[1]));

		assertEquals(getReq("(osgi.identity=b)"), r.get(0));
		assertEquals(getReq("(osgi.identity=c)"), r.get(1));
	}

	private static Requirement getReq(String n) {
		return cp.addDirective(Namespace.REQUIREMENT_FILTER_DIRECTIVE, n)
			.buildSyntheticRequirement();
	}

	public static void testVariableInSystemPackages() throws Exception {
		Workspace ws = new Workspace(new File("testresources/ws"));
		BndEditModel model = new BndEditModel(ws);
		File f = new File("testresources/ws/p7/syspkg.bndrun");
		model.setBndResource(f);
		model.setBndResourceName("syspkg.bndrun");
		model.loadFrom(f);

		List<ExportedPackage> ep = model.getSystemPackages();

		assertEquals("com.sun.xml.internal.bind", model.getProperties()
			.mergeProperties(Constants.RUNSYSTEMPACKAGES));

		ExportedPackage e = new ExportedPackage("testing", null);
		ep = new LinkedList<>();
		ep.add(e);

		model.setSystemPackages(ep);

		ep = model.getSystemPackages();
		assertEquals(1, ep.size());
		assertEquals("testing", ep.get(0)
			.getName());

		e = new ExportedPackage("${var}", null);
		ep = new LinkedList<>();
		ep.add(e);

		model.setSystemPackages(ep);

		ep = model.getSystemPackages();
		assertEquals(1, ep.size());
		assertEquals("com.sun.xml.internal.bind", model.getProperties()
			.mergeProperties(Constants.RUNSYSTEMPACKAGES));
	}

	public static void testRunReposShared() throws Exception {
		Workspace ws = new Workspace(new File("testresources/ws"));
		BndEditModel model = new BndEditModel(ws);
		File f = new File("testresources/ws/p7/syspkg.bndrun");
		model.setBndResource(f);
		model.setBndResourceName("syspkg.bndrun");
		model.loadFrom(f);

		List<String> runrepos = model.getRunRepos();
		assertEquals(1, runrepos.size());
		assertEquals("testing", runrepos.get(0));
	}

	public static void testRemovePropertyFromStandalone() throws Exception {
		File runFile = IO.getFile("testresources/standalone.bndrun");
		Run run = Run.createRun(null, runFile);

		BndEditModel model = new BndEditModel();
		model.setWorkspace(run.getWorkspace());
		model.loadFrom(runFile);

		assertEquals("A", model.getProperties()
			.get("a"));
		assertEquals("B", model.getProperties()
			.get("b"));
		assertEquals("C", model.getProperties()
			.get("c"));

		String newContent = "-standalone\n" + "a: A\n" + "c: C"; // remove b
		model.loadFrom(new ByteArrayInputStream(newContent.getBytes()));

		assertEquals("A", model.getProperties()
			.get("a"));
		assertNull("removed property should be null", model.getProperties()
			.get("b"));
		assertEquals("C", model.getProperties()
			.get("c"));
	}

	public void testGetProperties() throws Exception {
		File runFile = IO.getFile("testresources/standalone.bndrun");
		Run run = Run.createRun(null, runFile);
		BndEditModel model = new BndEditModel(run);

		assertThat(model.getWorkspace()).isNotNull();
		assertThat(model.getProject()).isNotNull();

		model.setGenericString("a", "AA");

		Processor properties = model.getProperties();

		assertThat(properties.getProperty("a")).isEqualTo("AA");
		assertThat(run.getProperty("a")).isEqualTo("A");

		assertThat(properties.getPropertiesFile()).isEqualTo(run.getPropertiesFile());
		assertThat(properties.getBase()).isEqualTo(run.getBase());

	}

	public void testGetPropertiesWithFileReplace() throws Exception {
		File runFile = IO.getFile("testresources/standalone.bndrun");
		Run run = Run.createRun(null, runFile);
		BndEditModel model = new BndEditModel(run);

		model.setGenericString("here.also", "${.}");

		Processor properties = model.getProperties();

		assertThat(properties.getProperty("here")).isEqualTo(getPortablePath(runFile.getParentFile()));

		assertThat(properties.getProperty("here.also")).isEqualTo(getPortablePath(runFile.getParentFile()));
	}

	public void testGetPropertiesWithWorkspaceMacros() throws Exception {
		Workspace ws = new Workspace(new File("testresources/ws"));
		Project project = ws.getProject("p1");
		BndEditModel model = new BndEditModel(project);
		model.setGenericString("ws", "${workspace}");
		model.setGenericString("pro", "${project}");
		Processor p = model.getProperties();

		assertThat(p.getProperty("ws")).isEqualTo(getPortablePath(ws.getBase()));

		assertThat(p.getProperty("pro")).isEqualTo(getPortablePath(project.getBase()));
	}

	public void testGetPropertiesWithoutParent() throws Exception {
		BndEditModel model = new BndEditModel();
		model.setGenericString("foo", "FOO");
		Processor p = model.getProperties();

		assertThat(p.getProperty("foo")).isEqualTo("FOO");
	}

	public void testGetPropertiesWithOnlyWorkspace() throws Exception {
		Workspace ws = new Workspace(new File("testresources/ws"));
		BndEditModel model = new BndEditModel(ws);
		model.setGenericString("foo", "FOO");
		Processor p = model.getProperties();

		assertThat(p.getProperty("foo")).isEqualTo("FOO");
	}

	private String getPortablePath(File base) {
		String path = base.getAbsolutePath();
		if (File.separatorChar != '/') {
			path = path.replace(File.separatorChar, '/');
		}
		return path;
	}

}
