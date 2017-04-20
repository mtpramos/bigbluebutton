package org.bigbluebutton.core.apps

import org.bigbluebutton.core.util.RandomStringGenerator
import org.bigbluebutton.core.api.Presenter
import org.bigbluebutton.core.models.{ RegisteredUser, Roles, UserVO }

class UsersModel {
  private var uservos = new collection.immutable.HashMap[String, UserVO]

  private var regUsers = new collection.immutable.HashMap[String, RegisteredUser]

  /* When reconnecting SIP global audio, users may receive the connection message
   * before the disconnection message.
   * This variable is a connection counter that should control this scenario.
   */
  private var globalAudioConnectionCounter = new collection.immutable.HashMap[String, Integer]

  private var locked = false
  private var meetingMuted = false
  private var recordingVoice = false

  private var currentPresenter = new Presenter("system", "system", "system")

  def setCurrentPresenterInfo(pres: Presenter) {
    currentPresenter = pres
  }

  def getCurrentPresenterInfo(): Presenter = {
    currentPresenter
  }

  def addRegisteredUser(token: String, regUser: RegisteredUser) {
    regUsers += token -> regUser
  }

  def getRegisteredUserWithToken(token: String, userId: String): Option[RegisteredUser] = {

    def isSameUserId(ru: RegisteredUser, userId: String): Option[RegisteredUser] = {
      if (userId.startsWith(ru.id)) {
        Some(ru)
      } else {
        None
      }
    }

    for {
      ru <- regUsers.get(token)
      user <- isSameUserId(ru, userId)
    } yield user

  }

  def generateWebUserId: String = {
    val webUserId = RandomStringGenerator.randomAlphanumericString(6)
    if (!hasUser(webUserId)) webUserId else generateWebUserId
  }

  def addUser(uvo: UserVO) {
    uservos += uvo.id -> uvo
  }

  def removeUser(userId: String): Option[UserVO] = {
    val user = uservos get (userId)
    user foreach (u => uservos -= userId)

    user
  }

  def hasSessionId(sessionId: String): Boolean = {
    uservos.contains(sessionId)
  }

  def hasUser(userID: String): Boolean = {
    uservos.contains(userID)
  }

  def numUsers(): Int = {
    uservos.size
  }

  def numWebUsers(): Int = {
    uservos.values filter (u => u.phoneUser == false) size
  }

  def numUsersInVoiceConference: Int = {
    val joinedUsers = uservos.values filter (u => u.voiceUser.joined)
    joinedUsers.size
  }

  def getUserWithExternalId(userID: String): Option[UserVO] = {
    uservos.values find (u => u.externalId == userID)
  }

  def getUserWithVoiceUserId(voiceUserId: String): Option[UserVO] = {
    uservos.values find (u => u.voiceUser.userId == voiceUserId)
  }

  def getUser(userID: String): Option[UserVO] = {
    uservos.values find (u => u.id == userID)
  }

  def getUsers(): Array[UserVO] = {
    uservos.values toArray
  }

  def numModerators(): Int = {
    getModerators.length
  }

  def findAModerator(): Option[UserVO] = {
    uservos.values find (u => u.role == Roles.MODERATOR_ROLE)
  }

  def noPresenter(): Boolean = {
    !getCurrentPresenter().isDefined
  }

  def getCurrentPresenter(): Option[UserVO] = {
    uservos.values find (u => u.presenter == true)
  }

  def unbecomePresenter(userID: String) = {
    uservos.get(userID) match {
      case Some(u) => {
        val nu = u.copy(presenter = false)
        uservos += nu.id -> nu
      }
      case None => // do nothing	
    }
  }

  def becomePresenter(userID: String) = {
    uservos.get(userID) match {
      case Some(u) => {
        val nu = u.copy(presenter = true)
        uservos += nu.id -> nu
      }
      case None => // do nothing	
    }
  }

  def getModerators(): Array[UserVO] = {
    uservos.values filter (u => u.role == Roles.MODERATOR_ROLE) toArray
  }

  def getViewers(): Array[UserVO] = {
    uservos.values filter (u => u.role == Roles.VIEWER_ROLE) toArray
  }

  def isModerator(userId: String): Boolean = {
    uservos.get(userId) match {
      case Some(user) => return user.role == Roles.MODERATOR_ROLE && !user.waitingForAcceptance
      case None => return false
    }
  }

  def getRegisteredUserWithUserID(userID: String): Option[RegisteredUser] = {
    regUsers.values find (ru => userID contains ru.id)
  }

  def removeRegUser(userID: String) {
    getRegisteredUserWithUserID(userID) match {
      case Some(ru) => {
        regUsers -= ru.authToken
      }
      case None =>
    }
  }

  def updateRegUser(uvo: UserVO) {
    getRegisteredUserWithUserID(uvo.id) match {
      case Some(ru) => {
        val regUser = new RegisteredUser(uvo.id, uvo.externalId, uvo.name, uvo.role, ru.authToken,
          uvo.avatarURL, uvo.guest, uvo.authed, uvo.waitingForAcceptance)
        regUsers -= ru.authToken
        regUsers += ru.authToken -> regUser
      }
      case None =>
    }
  }

  def addGlobalAudioConnection(userID: String): Boolean = {
    globalAudioConnectionCounter.get(userID) match {
      case Some(vc) => {
        globalAudioConnectionCounter += userID -> (vc + 1)
        false
      }
      case None => {
        globalAudioConnectionCounter += userID -> 1
        true
      }
    }
  }

  def removeGlobalAudioConnection(userID: String): Boolean = {
    globalAudioConnectionCounter.get(userID) match {
      case Some(vc) => {
        if (vc == 1) {
          globalAudioConnectionCounter -= userID
          true
        } else {
          globalAudioConnectionCounter += userID -> (vc - 1)
          false
        }
      }
      case None => {
        false
      }
    }
  }

  def startRecordingVoice() {
    recordingVoice = true
  }

  def stopRecordingVoice() {
    recordingVoice = false
  }

  def isVoiceRecording: Boolean = {
    recordingVoice
  }
}