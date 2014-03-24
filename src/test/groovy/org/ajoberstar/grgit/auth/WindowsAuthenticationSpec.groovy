package org.ajoberstar.grgit.auth

import spock.lang.Specification
import spock.lang.Unroll

import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Person
import org.ajoberstar.grgit.service.RepositoryService
import org.ajoberstar.grgit.fixtures.WindowsSpecific
import org.ajoberstar.grgit.fixtures.GitTestUtil

import org.junit.Rule
import org.junit.experimental.categories.Category
import org.junit.rules.TemporaryFolder

@Category(WindowsSpecific)
class WindowsAuthenticationSpec extends Specification {
	private static final String SSH_URI = 'git@github.com:ajoberstar/grgit-test.git'
	private static final String HTTPS_URI = 'https://github.com/ajoberstar/grgit-test.git'

	static Credentials hardcodedCreds

	@Rule TemporaryFolder tempDir = new TemporaryFolder()

	RepositoryService grgit
	Person person = new Person('Bruce Wayne', 'bruce.wayne@wayneindustries.com')


	def setupSpec() {
		def username = System.properties['org.ajoberstar.grgit.test.username']
		def password = System.properties['org.ajoberstar.grgit.test.password']
		hardcodedCreds = new Credentials(username, password)
		assert hardcodedCreds.username && hardcodedCreds.password
	}

	def cleanup() {
		System.properties.remove(AuthConfig.FORCE_OPTION)
	}

	@Unroll('#method works')
	def 'auth method works'() {
		given:
		assert System.properties[AuthConfig.FORCE_OPTION] == null, 'Force should not already be set.'
		System.properties[AuthConfig.FORCE_OPTION] = method
		ready(ssh, creds)
		assert grgit.branch.status(branch: 'master').aheadCount == 1
		when:
		grgit.push()
		then:
		grgit.branch.status(branch: 'master').aheadCount == 0
		where:
		method        | ssh   | creds
		'hardcoded'   | false | hardcodedCreds
		'interactive' | false | null
		'interactive' | true  | null
		'sshagent'    | true  | null
		'pageant'     | true  | null
	}

	private void ready(boolean ssh, Credentials credentials = null) {
		File repoDir = tempDir.newFolder('repo')
		String uri = ssh ? SSH_URI : HTTPS_URI
		grgit = Grgit.clone(uri: uri, dir: repoDir, credentials: credentials)
		grgit.repository.git.repo.config.with {
			setString('user', null, 'name', person.name)
			setString('user', null, 'email', person.email)
			save()
		}

		repoFile('windows_test.txt') << "${new Date().format('yyyy-MM-dd')}\n"
		grgit.add(patterns: ['.'])
		grgit.commit(message: 'Windows auth test')
	}

	private File repoFile(String path, boolean makeDirs = true) {
		return GitTestUtil.repoFile(grgit, path, makeDirs)
	}
}
