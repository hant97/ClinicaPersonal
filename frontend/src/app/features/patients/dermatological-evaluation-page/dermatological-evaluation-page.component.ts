import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DermatologicalEvaluationListComponent } from '../dermatological-evaluation-list/dermatological-evaluation-list.component';

@Component({
  selector: 'app-dermatological-evaluation-page',
  standalone: true,
  imports: [RouterLink, DermatologicalEvaluationListComponent],
  templateUrl: './dermatological-evaluation-page.component.html'
})
export class DermatologicalEvaluationPageComponent implements OnInit {
  patientId: number | null = null;

  constructor(private route: ActivatedRoute) {}

  ngOnInit(): void {
    const patientId = Number(this.route.snapshot.paramMap.get('patientId'));
    this.patientId = Number.isInteger(patientId) && patientId > 0 ? patientId : null;
  }
}
