import * as React from "react"
import { connect, Dispatch } from "react-redux"
import * as NewFolderActions from "newFolder/NewFolderActions"
import { NewFolderState } from "newFolder/NewFolderReducer"
import { GlobalState } from "store"
import { Directory } from "models/FsNode"
import NewFolderFrom from "newFolder/NewFolderFrom"
import Modal from "components/modals/Modal"
import ModalActions from "components/modals/ModalActions"
import FlatButton from "components/buttons/FlatButton"

interface DispatchProps {
  onNewFolderNameChange: (newFolderName: string) => void
  onWantCreateNewFolder: () => void
  onCreateNewFolder: (directory: Directory, newFolderName: string) => void
}

interface PropsState extends NewFolderState {
  directory: Directory
}

type Props = PropsState & DispatchProps

class NewFolderContainer extends React.PureComponent<Props> {
  render() {
    const { wantCreateNewFolder, onWantCreateNewFolder } = this.props
    return (
      <div>
        <FlatButton label="New Folder" onClick={onWantCreateNewFolder} />
        {wantCreateNewFolder ? this.renderModal() : null}
      </div>
    )
  }

  renderModal = () => {
    const { newFolderName, onNewFolderNameChange, onWantCreateNewFolder } = this.props
    return (
      <Modal
        title="Create new directory"
        onClose={onWantCreateNewFolder}
      >
        <NewFolderFrom
          name={newFolderName}
          onChange={onNewFolderNameChange}
        />
        <ModalActions>
          <FlatButton label="Cancel" onClick={onWantCreateNewFolder} />
          <FlatButton label="Create" onClick={this.handleOnSubmit} />
        </ModalActions>
      </Modal>
    )
  }

  handleOnSubmit = () => {
    const { newFolderName, directory, onCreateNewFolder } = this.props
    onCreateNewFolder(directory, newFolderName)
  }
}

const mapStateToProps = (state: GlobalState): PropsState => {
  return {
    ...state.newFolder,
    directory: state.directories.directory!
  }
}
const mapDispatchToProps = (dispatch: Dispatch<GlobalState>): DispatchProps => {
  return {
    onNewFolderNameChange: newFolderName => dispatch(NewFolderActions.onNewFolderNameChange(newFolderName)),
    onWantCreateNewFolder: () => dispatch(NewFolderActions.onWantCreateNewFolder()),
    onCreateNewFolder: (directory, newFolderName) => {
      dispatch(NewFolderActions.onCreateNewFolder(directory, newFolderName))
    },
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(NewFolderContainer)
