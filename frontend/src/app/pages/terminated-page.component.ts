import { Component } from '@angular/core';

@Component({
  selector: 'app-terminated-page',
  standalone: true,
  template: `
    <h2>Account Access Ended</h2>
    <p>Your employment record indicates you are no longer active.</p>
    <p>Please contact HR for assistance.</p>
  `
})
export class TerminatedPageComponent {}
